package u2gfe;

import static u2gfe.PanelBinFile.*;
import static u2gfe.Structures.*;
import static u2gfe.Util.*;
import static u2gfe.WindowMain.fmaxascend;
import static u2gfe.WindowMain.fx;
import static u2gfe.WindowMain.fy;
import static u2gfe.WindowMain.monospace;
import static u2gfe.WindowMain.updateFontMetrics;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import u2gfe.BinFile.Section;

class PanelBinFile extends JComponent implements
	ComponentListener, AdjustmentListener, MouseListener, MouseMotionListener,
	MouseWheelListener, ActionListener
{
static int grid(int x, int y) { return (x << 16) | y; }
static int gridx(int grid) { return grid >>> 16; }
static int gridy(int grid) { return grid & 0xFFFF; }

BinFile file;
JScrollBar scrollHoriz, scrollVert;
/** This is not displayed; its children are (in {@link #lines}) */
SectionPart rootSection;
ArrayList<Part> lines;
HashMap<Integer, String[]> hoverData = new HashMap<>();
HashMap<Integer, Part> clickData = new HashMap<>();
int mouseX = -100, mouseY = -100;
String tooltipText[];
int lastHoveredGrid;
int offsetToHighlightOnOpen;
boolean needScrollToOffsetToHighlightOnOpen;
Part highlightedPart;
long lastHighlightRender;
boolean highlightFlashingPhase;
Timer timer;
boolean isDraggingDivider;
Symbol hoveringSymbol;

PanelBinFile(BinFile file, JScrollBar scrollHoriz, JScrollBar scrollVert, int scrollToOffset)
{
	super();
	this.file = file;
	this.scrollHoriz = scrollHoriz;
	this.scrollVert = scrollVert;
	this.offsetToHighlightOnOpen = scrollToOffset;
	if (scrollToOffset != 0) {
		needScrollToOffsetToHighlightOnOpen = true;
	}
	this.recalcHexCharsPerRow();

	scrollHoriz.addAdjustmentListener(this);
	scrollVert.addAdjustmentListener(this);

	this.addComponentListener(this);
	this.addMouseMotionListener(this);
	this.addMouseWheelListener(this);
	this.addMouseListener(this);

	this.rootSection = new SectionPart(null);
	this.rootSection.nestLevel = -1;
	for (Section section : file.sections) {
		addSection(this.rootSection, section);
	}
	this.lines = this.rootSection.children;

	this.timer = new Timer(100, this);
	this.timer.start();
}

void addSection(SectionPart parent, Section section)
{
	String text = String.format("section %Xh (size %Xh)", section.magic, section.size);
	String name = Structures.get_name(section.magic);
	if (name != null) {
		text += ": " + name;
	}
	SectionPart s = new SectionPart(new Label(text));
	s.startOffset = section.offset - 8;
	s.endOffset = section.offset;
	s.nestLevel = parent.nestLevel + 1;
	s.parent = parent;
	parent.children.add(s);
	if (section.subsections != null) {
		for (Section sub : section.subsections) {
			addSection(s, sub);
		}
	} else {
		addFields(s, section.fields, 0);
	}
}

int addFields(SectionPart parent, Object fields[], int fromIndex)
{
	// skip MARK_STRUCT_START
	// skip size as parent should've used it already
	// skip name as parent should've used it already
	fromIndex += 3;
	while (fromIndex < fields.length) {
		Object next = fields[fromIndex];
		if (next == MARK_STRUCT_END) {
			// skip result (TODO: add a field if it's UNEXPECTED END?)
			return fromIndex + 2;
		} else if (next == MARK_STRUCT_START) {
			int size = (int) fields[fromIndex + 1];
			String structName = (String) fields[fromIndex + 2];
			String text = String.format("%s (%Xh)", structName, size);
			SectionPart s = new SectionPart(new Label(text));
			s.nestLevel = parent.nestLevel + 1;
			s.size = size;
			s.parent = parent;
			parent.children.add(s);
			fromIndex = this.addFields(s, fields, fromIndex);
		} else {
			Part s = null;
			boolean didAdd = false;
			int fieldtype = (int) fields[fromIndex + 1];
			int startOffset = (int) fields[fromIndex + 2];
			int endOffset = (int) fields[fromIndex + 3];

			String text = (String) next;
			if (text == null) text = "(noname)";
			text = String.format("%3Xh %s", endOffset - startOffset, text);
			String data = null;

			switch (fieldtype) {
			case T_STR:
				data = "\"" + cstr(file.data, startOffset, endOffset) + "\"";
				break;
			case T_I32:
				int i = i32(file.data, startOffset);
				data = String.format("%d (%Xh)", i, i);
				break;
			case T_I16:
				int sh = i16(file.data, startOffset);
				data = String.format("%d (%Xh)", sh, sh);
				break;
			case T_I8:
			{
				int c = i8(file.data, startOffset);
				data = String.format("%d (%Xh)", c, c);
				break;
			}
			case T_F32:
				data = String.valueOf(f32(file.data, startOffset));
				break;
			case T_PADDING:
				data = "(padding)";
				break;
			case T_HASHREF:
				int hr = i32(file.data, startOffset);
				data = String.format("HASH REF %08X", hr, hr);
				break;
			case T_HASH:
				int h = i32(file.data, startOffset);
				data = String.format("HASH %08X", h, h);
				break;
			case T_CAREERSTRPOOL:
			{
				int at = startOffset;
				int to = endOffset;
				for (int entryFrom = at; at < to; at++) {
					byte c = file.data[at];
					if (c == 0 || at == to) {
						String val = cstr(file.data, entryFrom, at);
						int offset = entryFrom - startOffset;
						s = new Label(String.format("offset %4Xh: %s", offset, val));
						s.nestLevel = parent.nestLevel + 1;
						s.startOffset = entryFrom;
						s.endOffset = at + 1;
						s.parent = parent;
						parent.children.add(s);
						entryFrom = at + 1;
					}
				}
				didAdd = true;
				break;
			}
			case T_CAREERSTRPOOLREF:
				int offset = i16(file.data, startOffset);
				s = new LabelCareerString(text, offset);
				break;
			case T_UNK:
			case T_OBJLINK:
				break;
			default:
				throw new RuntimeException(String.format("unexpected type: %X", fieldtype));
			}
			if (!didAdd) {
				if (s == null) {
					if (data != null) text += ": " + data;
					s = new Label(text);
				}
				s.nestLevel = parent.nestLevel + 1;
				s.startOffset = startOffset;
				s.endOffset = endOffset;
				s.parent = parent;
				parent.children.add(s);
			}
			fromIndex += 4;
		}
	}
	return fromIndex;
}

void updateScrollbars()
{
	int w = this.getWidth(), h = this.getHeight();
	int fullRowsVisible = h / fy, fullColsVisible = w / fx - HEX_CHARS_PER_ROW - 1;

	int oldVertValue = this.scrollVert.getValue();
	if (this.needScrollToOffsetToHighlightOnOpen) {
		this.needScrollToOffsetToHighlightOnOpen = false;
		ArrayDeque<Part> parts = new ArrayDeque<>(this.lines);
		while (!parts.isEmpty()) {
			Part p = parts.pop();
			if (p.startOffset <= this.offsetToHighlightOnOpen &&
				this.offsetToHighlightOnOpen < p.endOffset)
			{
				// gotcha
				this.highlightedPart = p;
				int lineOffset = 0;
				SectionPart parent = p.parent;
				ArrayDeque<Integer> childIndices = new ArrayDeque<>();
				while (parent != null) {
					for (int i = 0; i < parent.children.size(); i++) {
						Part child = parent.children.get(i);
						if (child == p) {
							if (child instanceof SectionPart) {
								childIndices.push(i);
							}
							break;
						}
						lineOffset += child.getHeight();
					}
					p = parent;
					parent = p.parent;
				}
				oldVertValue = Math.max(0, lineOffset - fullRowsVisible / 2);
				int absoluteIdx = 0;
				parent = this.rootSection;
				while (!childIndices.isEmpty()) {
					int idx = childIndices.pop();
					absoluteIdx += idx;
					parent = (SectionPart) parent.children.get(idx);
					if (!parent.isExpanded) {
						parent.isExpanded = true;
						this.lines.addAll(++absoluteIdx, parent.children);
					}
				}
				break;
			}
			if (p instanceof SectionPart) {
				parts.addAll(((SectionPart) p).children);
			}
		}
	}

	int totalCols = 0, totalRows = 0;
	for (Part line : this.lines) {
		totalCols = Math.max(totalCols, line.getWidth());
		totalRows += line.getHeight();
	}
	int hiddenRows = totalRows - fullRowsVisible, hiddenCols = totalCols - fullColsVisible;
	if (hiddenRows > 0) {
		int extent = fullRowsVisible;
		int newMax = hiddenRows;
		int newValue = Math.min(newMax, oldVertValue);
		this.scrollVert.setValues(newValue, extent, 0, newMax + extent);
		this.scrollVert.setEnabled(true);
	} else {
		this.scrollVert.setValues(0, fullRowsVisible, 0, fullRowsVisible);
		this.scrollVert.setEnabled(false);
	}
	if (hiddenCols > 0) {
		int extent = fullColsVisible;
		int newMax = hiddenCols;
		int newValue = Math.min(newMax, this.scrollHoriz.getValue());
		this.scrollHoriz.setValues(newValue, extent, 0, newMax + extent);
		this.scrollHoriz.setEnabled(true);
	} else {
		this.scrollHoriz.setValues(0, fullColsVisible, 0, fullColsVisible);
		this.scrollHoriz.setEnabled(false);
	}
}

@Override
protected void paintComponent(Graphics g)
{
	int w = this.getWidth(), h = this.getHeight();
	updateFontMetrics(g);
	g.setFont(monospace);
	g.setColor(Color.white);
	g.fillRect(0, 0, w, h);
	g.setColor(Color.black);

	this.hoveringSymbol = null;
	this.hoverData.clear();
	this.clickData.clear();
	int colOffset = -this.scrollHoriz.getValue();
	int currentRow = -this.scrollVert.getValue();
	int rowsVisible = h / fy + 1; // +1 in case there's space for a partial extra line
	Point mouse = new Point(this.mouseX / fx, this.mouseY / fy);
	for (Part line : this.lines) {
		int lineHeight = line.getHeight();
		currentRow += lineHeight;
		if (currentRow < 0) {
			continue;
		}
		if (this.highlightFlashingPhase && line == this.highlightedPart) {
			g.setColor(Color.yellow);
			g.fillRect(0, (currentRow - lineHeight) * fy, w, lineHeight * fy);
			g.setColor(Color.black);
		}
		{ // hex
			int offset = line.startOffset;
			int row = currentRow - lineHeight;
			int bytes = line.endOffset - line.startOffset;
			if (row < 0) {
				bytes += row * HEX_BYTES_PER_ROW;
				offset += -row * HEX_BYTES_PER_ROW;
				row = 0;
			}
			// highlight hovered byte (both hex & ascii)
			int x = gridx(this.lastHoveredGrid);
			int y = gridy(this.lastHoveredGrid);
			if (row <= y && y < currentRow && x < HEX_BYTES_PER_ROW * 4 + 1) {
				int offs = (y - row) * HEX_BYTES_PER_ROW;
				int xx = -1;
				if (x < HEX_BYTES_PER_ROW * 3 && (x % 3) != 2) {
					xx = x / 3;
				} else if (x > HEX_BYTES_PER_ROW * 3) {
					xx = x - HEX_BYTES_PER_ROW * 3 - 1;
				}
				offs += xx;
				if (xx != -1 && offs < bytes) {
					int off = line.startOffset + offs;
					int i8 = i8(this.file.data, off);
					int i16 = i16(this.file.data, off);
					int i32 = i32(this.file.data, off);
					Symbol sym = Symbol.symbols.get(i32);
					this.hoveringSymbol = sym;
					this.tooltipText = new String[] {
						String.format("abs offset %X", off),
						String.format("rel offset %X", off - line.parent.startOffset),
						String.format("byte %d (%Xh) word %d (%Xh)", i8, i8, i16, i16 & 0xFFFF),
						String.format("dword %d (%Xh)", i32, i32),
						String.format("resolved hash: %s", sym == null ? "no" : sym.kind)
					};
					g.setColor(Color.CYAN);
					g.fillRect((xx * 3) * fx, y * fy, fx * 2, fy);
					g.fillRect((HEX_BYTES_PER_ROW * 3 + 1 + xx) * fx, y * fy, fx, fy);
					g.setColor(Color.black);
				}
			}
			// hex & ascii text
			while (bytes > 0 && row < rowsVisible) {
				int linebytes = Math.min(HEX_BYTES_PER_ROW, bytes);
				for (int i = 0; i < linebytes; i++) {
					String b = String.format("%02X", this.file.data[offset + i]);
					g.drawString(b, (i * 3) * fx, row * fy + fmaxascend);
				}
				for (int i = 0; i < linebytes; i++) {
					byte b = this.file.data[offset + i];
					String bb = ".";
					if (b > 32 && b < 127) {
						bb = String.format("%c", b);
					}
					g.drawString(bb, (HEX_BYTES_PER_ROW * 3 + 1 + i) * fx, row * fy + fmaxascend);
				}
				bytes -= linebytes;
				offset += linebytes;
				row++;
			}
		}
		int clipw = (HEX_CHARS_PER_ROW + 1) * fx;
		g.setClip(clipw, 0, w - clipw, h);
		int x = line.nestLevel * 3 + colOffset + HEX_CHARS_PER_ROW + 1;
		{ // guide lines
			int xx = x;
			boolean isParent = false;
			Part pp = line;
			g.setColor(new Color(0xaaaaff));
			while (pp != null && pp.parent != this.rootSection) {
				xx -= 2;
				int a = xx * fx + fx / 2;
				int b = a + fx;
				int c = (currentRow - lineHeight) * fy;
				int e = currentRow * fy;
				if (isParent) {
					if (!pp.isLastChild()) {
						g.drawLine(a, c, a, e);
					}
				} else {
					int d = c + fy / 2;
					g.drawLine(a, c, a, d);
					g.drawLine(a, d, b, d);
					if (!pp.isLastChild()) {
						g.drawLine(a, d, a, e);
					}
				}
				pp = pp.parent;
				isParent = true;
				xx -= 1;
			}
			g.setColor(Color.black);
		}
		line.render(g, x, currentRow - lineHeight, mouse, this.hoverData, this.clickData);
		g.setClip(0, 0, w, h);

		if (currentRow > rowsVisible) {
			break;
		}
	}
	{ // divider
		int x = HEX_CHARS_PER_ROW * fx;
		g.fillRect(x + fx / 2 - 1, 0, 2, h);
	}
	if (this.tooltipText != null) {
		int width = 0, height = (this.tooltipText.length * fy) + 4;
		for (String line : this.tooltipText) {
			width = Math.max(width, line.length());
		}
		width = width * fx + 4;
		int fromX = this.mouseX - width / 2, fromY = this.mouseY - 6 - height;
		if (fromY < 0) {
			fromY = this.mouseY + 20;
		}
		if (fromX < 2) {
			fromX = 2;
		}
		if (fromX + width > w - 2) {
			fromX = w - 2 - width;
		}
		g.translate(fromX, fromY);
		g.setColor(Color.black);
		g.drawRect(0, 0, width - 1, height - 1);
		g.translate(1, 1);
		g.setColor(Color.yellow);
		g.fillRect(0, 0, width - 2, height - 2);
		g.translate(1, 1);
		g.setColor(Color.black);
		for (String line : this.tooltipText) {
			g.drawString(line, 0, fmaxascend);
			g.translate(0, fy);
		}
	}
}

/*ComponentListener*/
@Override
public void componentResized(ComponentEvent e)
{
	this.updateScrollbars();
}

/*ComponentListener*/
@Override
public void componentMoved(ComponentEvent e)
{
}

/*ComponentListener*/
@Override
public void componentShown(ComponentEvent e)
{
}

/*ComponentListener*/
@Override
public void componentHidden(ComponentEvent e)
{
}

/*AdjustmentListener*/
@Override
public void adjustmentValueChanged(AdjustmentEvent e)
{
	this.repaint();
}

static int HEX_BYTES_PER_ROW = 16;
static int HEX_CHARS_PER_ROW;
void recalcHexCharsPerRow()
{
	HEX_CHARS_PER_ROW =
		// don't change this formula without also checking the mousedrag behavior!
		// Also mousehover tooltip
		(HEX_BYTES_PER_ROW * 3) + // hex numbers + spaces
		1 + // extra space between hex numbers and ascii representation
		HEX_BYTES_PER_ROW + // ascii representation
		2 + // extra padding for some reason
		0;
}

/*MouseMotionListener*/
@Override
public void mouseDragged(MouseEvent e)
{
	int x = e.getX() / fx;
	if (this.isDraggingDivider) {
		this.lastHoveredGrid = -1;
		int w = x - 3;
		HEX_BYTES_PER_ROW = Math.max((w + 2) / 4, 8);
		this.recalcHexCharsPerRow();
		this.mouseX = e.getX();
		this.mouseY = e.getY();
		this.tooltipText = new String[] { String.format("%d bytes per row", HEX_BYTES_PER_ROW) };
		this.updateScrollbars();
		this.repaint();
	} else {
		this.mouseMoved(e);
	}
}

/*MouseMotionListener*/
@Override
public void mouseMoved(MouseEvent e)
{
	int x = e.getX() / fx, y = e.getY() / fy;
	if (x == HEX_CHARS_PER_ROW) {
		this.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
	} else if (this.getCursor() != null) {
		this.setCursor(null);
	}
	int grid = grid(x, y);
	this.mouseX = e.getX();
	this.mouseY = e.getY();
	String[] tooltipText = this.hoverData.get(grid);
	if (tooltipText != null) {
		this.tooltipText = tooltipText;
		this.lastHoveredGrid = grid;
		this.repaint();
		return;
	} else if (this.tooltipText != null) {
		this.tooltipText = null;
		this.lastHoveredGrid = grid;
		this.repaint();
		return;
	} else if (this.lastHoveredGrid != grid) {
		this.lastHoveredGrid = grid;
		this.repaint();
		return;
	}
}

/*MouseListener*/
@Override
public void mouseClicked(MouseEvent e)
{
}

/*MouseListener*/
@Override
public void mousePressed(MouseEvent e)
{
	int x = e.getX() / fx;
	if (x == HEX_CHARS_PER_ROW) {
		this.isDraggingDivider = true;
	}
}

/*MouseListener*/
@Override
public void mouseReleased(MouseEvent e)
{
	if (this.isDraggingDivider) {
		this.isDraggingDivider = false;
		this.tooltipText = null;
		this.setCursor(null);
		this.repaint();
	}
	if (this.hoveringSymbol != null) {
		Messages.SHOW_BINFILE_AT_OFFSET.send(new Messages.ShowBinFileAtOffset(this.hoveringSymbol));
		this.hoveringSymbol = null;
		return;
	}
	int x = e.getX() / fx, y = e.getY() / fy;
	Part clickedPart = this.clickData.get(grid(x, y));
	if (clickedPart instanceof SectionPart) {
		SectionPart cs = (SectionPart) clickedPart;
		int idx = this.lines.indexOf(cs);
		if (idx != -1) {
			if (cs.isExpanded) {
				ArrayDeque<SectionPart> csToRemove = new ArrayDeque<>();
				csToRemove.push(cs);
				while (!csToRemove.isEmpty()) {
					SectionPart c = csToRemove.pop();
					c.isExpanded = false;
					this.lines.removeAll(c.children);
					for (Part child : c.children) {
						if (child instanceof SectionPart) {
							csToRemove.push((SectionPart) child);
						}
					}
				}
			} else {
				cs.isExpanded = true;
				this.lines.addAll(idx + 1, cs.children);
			}
			this.updateScrollbars();
			this.repaint();
		}
	} else if (clickedPart instanceof LabelCareerString) {
		int offset = ((LabelCareerString) clickedPart).offset;
		BinFile file = CareerStringPool.originFile;
		String title = String.format("careerstringpool@%X", offset);
		offset += CareerStringPool.baseOffset;
		Messages.SHOW_BINFILE_AT_OFFSET.send(new Messages.ShowBinFileAtOffset(file, offset, title));
	} else if (clickedPart != null) {
		throw new RuntimeException();
	}
}

/*MouseListener*/
@Override
public void mouseEntered(MouseEvent e)
{
}

/*MouseListener*/
@Override
public void mouseExited(MouseEvent e)
{
	this.mouseX = -100;
	this.mouseY = -100;
	this.repaint();
}

/*MouseWheelListener*/
@Override
public void mouseWheelMoved(MouseWheelEvent e)
{
	int units = e.getUnitsToScroll();
	this.scrollVert.setValue(this.scrollVert.getValue() + units);
	this.repaint();
}

/*ActionListener*/
@Override
public void actionPerformed(ActionEvent e)
{
	if (e.getSource() == this.timer) {
		if (SwingUtilities.getWindowAncestor(this) != null) {
			if (this.highlightedPart != null) {
				long now = System.currentTimeMillis();
				if (now - this.lastHighlightRender >= 300) {
					this.lastHighlightRender = now;
					this.highlightFlashingPhase = !this.highlightFlashingPhase;
					this.repaint();
				}
			}
		} else {
			this.timer.stop();
		}
	}
}
} /*PanelBinFile*/

// ---

abstract class Part
{
int nestLevel;
int startOffset, endOffset, size;
SectionPart parent;

abstract void render(Graphics g, int x, int y, Point mouse, HashMap<Integer, String[]> hoverData, HashMap<Integer, Part> clickData);
abstract int getWidth();
final int getHeight()
{
	int bytes = this.endOffset - this.startOffset;
	int bytelines = bytes / HEX_BYTES_PER_ROW;
	if (bytes - bytelines * HEX_BYTES_PER_ROW > 0) {
		bytelines++;
	}
	return Math.max(getStructuredContentHeight(), bytelines);
}

int getStructuredContentHeight()
{
	return 1;
}

boolean isLastChild()
{
	return this.parent == null || this.parent.children.get(this.parent.children.size() - 1) == this;
}
} /*Part*/

// ---

class SectionPart extends Part
{
static Color expanded = new Color(0xff8888), collapsed = new Color(0xaaffaa), hovered = new Color(0xaaaaff);
ArrayList<Part> children = new ArrayList<>();
Part label;
boolean isExpanded;

SectionPart(Part label)
{
	this.label = label;
}

@Override
public void render(Graphics g, int x, int y, Point mouse, HashMap<Integer, String[]> hoverData, HashMap<Integer, Part> clickData)
{
	boolean isHovered = x <= mouse.x && mouse.x <= x + 2 && mouse.y == y;
	g.setColor(isHovered ? hovered : isExpanded ? expanded : collapsed);
	g.fillRect(x * fx, y * fy, fx * 3, fy);
	g.setColor(Color.black);
	g.drawString(isExpanded ? "-" : "+", x * fx + fx, y * fy + fmaxascend);
	clickData.put(grid(x, y), this);
	clickData.put(grid(x + 1, y), this);
	clickData.put(grid(x + 2, y), this);
	this.label.render(g, x + 3, y, mouse, hoverData, clickData);
}

@Override
public int getWidth()
{
	return 3 + this.label.getWidth();
}

@Override
public int getStructuredContentHeight()
{
	return this.label.getHeight();
}
} /*SectionPart*/

// ---

class Label extends Part
{
String contents;
int w;

Label(String contents)
{
	this.contents = contents;
	this.w = contents.length();
}

@Override
public void render(Graphics g, int x, int y, Point mouse, HashMap<Integer, String[]> hoverData, HashMap<Integer, Part> clickData)
{
	g.drawString(this.contents, x * fx, y * fy + fmaxascend);
}

@Override
public int getWidth()
{
	return this.w;
}
} /*Label*/

// ---

class LabelCareerString extends Part
{
static Color buttonbg = new Color(0x99ffff), hovered = new Color(0x66cccc);

String contents;
int offset;
int w;
boolean isResolved;

LabelCareerString(String baseText, int offset)
{
	this.offset = offset;
	String str = CareerStringPool.get(offset);
	this.contents = String.format("%s: %s", baseText, str);
	this.w = this.contents.length();
	this.isResolved = str != null;
	if (this.isResolved) {
		this.w += 5;
	}
}

@Override
public void render(Graphics g, int x, int y, Point mouse, HashMap<Integer, String[]> hoverData, HashMap<Integer, Part> clickData)
{
	g.drawString(this.contents, x * fx, y * fy + fmaxascend);
	if (this.isResolved) {
		int start = x + this.contents.length() + 1;
		boolean isHovered = start <= mouse.x && mouse.x <= start + 3 && mouse.y == y;
		g.setColor(isHovered ? hovered : buttonbg);
		g.fillRect(start * fx, y * fy, fx * 4, fy);
		g.setColor(Color.black);
		g.drawString("show", start * fx, y * fy + fmaxascend);
		clickData.put(grid(start, y), this);
		clickData.put(grid(start + 1, y), this);
		clickData.put(grid(start + 2, y), this);
		clickData.put(grid(start + 3, y), this);
	}
}

@Override
public int getWidth()
{
	return this.w;
}
} /*LabelCareerString*/

// ---