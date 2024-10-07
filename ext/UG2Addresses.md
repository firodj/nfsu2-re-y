* https://github.com/nlgzrgn/NFSScript/blob/87d5bf58b348330fc27cf6417795cc1dda7b4c59/NFSScript/Core/Addresses.cs#L193

```csharp
namespace NFSScript.Core
{
    /// <summary>
    ///
    /// </summary>
    public static class UG2Addresses
    {
        /// <summary>
        ///
        /// </summary>
        public struct GenericAddrs
        {
            /// <summary>
            ///
            /// </summary>
            public const int STATIC_IS_GAME_LOADED = 0x83152C;

            /// <summary>
            ///
            /// </summary>
            public const int STATIC_GAME_STATE = 0x8654A4;
            // 1,3 = Front End
            // 2,4,5 = Loading screen,
            // 6 = Racing
            // 7,8 = Exit Racing

            /// <summary>
            ///
            /// </summary>
            public const int STATIC_CHEAT_INPUT_FIELD = 0x865945;

            /// <summary>
            ///
            /// </summary>
            public const int STATIC_IS_GAMEPLAY_ACTIVE = 0x82BA74;

            /// <summary>
            ///
            /// </summary>
            public const int STATIC_IS_REPLAY_MODE = 0x8363E8;

            /// <summary>
            ///
            /// </summary>
            public const int STATIC_IS_REPLAY_MODE2 = 0x8650C0;

            /// <summary>
            ///
            /// </summary>
            public const int STATIC_LAST_FRAME_TIME = 0x865144;
        }

        /// <summary>
        ///
        /// </summary>
        public struct GameAddrs
        {
            /// <summary>
            ///
            /// </summary>
            public const int STATIC_IS_ACTIVITY_MODE = 0x88F2BE;
        }

        /// <summary>
        ///
        /// </summary>
        public struct WorldAddrs
        {
            /// <summary>
            ///
            /// </summary>
            public const int STATIC_WORLD_CARS_NEON_BRIGHTNESS = 0x60D8D0; // float
        }

        /// <summary>
        ///
        /// </summary>xxxxx
        public struct PlayerAddrs
        {
            /// <summary>
            ///
            /// </summary>
            public const int STATIC_PLAYER_CASH = 0x861E74;

            /// <summary>
            ///
            /// </summary>
            public const int STATIC_PLAYER_CAR_SPEED_MPH = 0x7F0A04;

            /// <summary>
            ///
            /// </summary>
            public const int STATIC_PLAYER_CURRENT_CAR_STAR_RATING = 0x8638F8;

            /// <summary>
            ///
            /// </summary>
            public const int STATIC_PLAYER_POSITION_READONLY_X = 0x8A2070;

            /// <summary>
            ///
            /// </summary>
            public const int STATIC_PLAYER_POSITION_READONLY_Y = 0x8A2078;

            /// <summary>
            ///
            /// </summary>
            public const int STATIC_PLAYER_POSITION_READONLY_Z = 0x8A2074;
        }

        /// <summary>
        ///
        /// </summary>
        public struct UIAddrs
        {
            /// <summary>
            ///
            /// </summary>
            public const int STATIC_CURSOR_POS_X = 0x838498;

            /// <summary>
            ///
            /// </summary>
            public const int STATIC_CURSOR_POS_Y = 0x83849C;
        }

        /// <summary>
        ///
        /// </summary>
        public struct OnlineAddrs
        {

        }
    }
}
```
