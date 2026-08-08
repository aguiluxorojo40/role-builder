package com.rolebuilder.core.snes

/**
 * Nombres CANÓNICOS de los sprites de Super Mario World por id (los de la
 * reimplementación snesrev/smw, `SprXXX_<Nombre>`, más los ids bajos que el juego
 * dibuja de forma genérica). Sirve para etiquetar en informes y en el editor qué
 * enemigo aparece en cada nivel. `nameOf` devuelve un fallback "Sprite 0xXX" si el id
 * no está catalogado.
 */
object SmwSpriteNames {

    val names: Map<Int, String> = mapOf(
        // ⚠ Estos ocho estaban INVERTIDOS: 0x00-0x03 figuraban como los Koopa normales y
        // 0x04-0x07 como los "NoShell". Es al revés, y se sostiene en tres cosas:
        //  · La tabla de despacho del juego (`kSprStatus08SpriteNormalPtrs`, banco $01) manda
        //    los ids 0x00-0x03 a `SprXXX_Generic_NakedKoopaEntry`, cuya rutina persigue y
        //    patea caparazones (`spr_current_status == 9/10`) — la conducta del beach koopa.
        //  · `kSprXXX_Generic_Spr0to13Prop`: el bit 0x40 (dibujar DOS teselas apiladas) NO
        //    está en 0x00-0x03, así que el juego los pinta con UNA sola tesela — y esa tesela,
        //    sacada de la ROM, es un bicho naranja SIN caparazón. Ese es el sprite entero.
        //  · `kSprStatus09_Stunned_SpriteKoopasSpawn` = {0,0,0,0,0,1,2,3} mapea 0x05→0x01,
        //    0x06→0x02, 0x07→0x03: de CON caparazón a SIN caparazón, mismo color.
        // El nombre equivocado no era inofensivo: al listar los sprites de un nivel decía
        // "RedKoopaNoShell" de los 8 Koopa rojos CON caparazón de YI-2 (0x106).
        0x00 to "GreenKoopaNoShell", 0x01 to "RedKoopaNoShell", 0x02 to "BlueKoopaNoShell",
        0x03 to "YellowKoopaNoShell",
        0x04 to "GreenKoopa", 0x05 to "RedKoopa", 0x06 to "BlueKoopa",
        0x07 to "YellowKoopa", 0x08 to "GreenParakoopa", 0x09 to "RedParakoopa",
        0x0A to "GreenFlyingParakoopa", 0x0B to "BobOmb", 0x0C to "BulletBillGenerator",
        0x0E to "Keyhole", 0x0F to "Goomba", 0x10 to "ParaGoomba",
        0x11 to "BuzzyBeetle", 0x13 to "KoopaKidBossFight", 0x14 to "SpinyEgg",
        0x16 to "VerticalCheepCheep", 0x17 to "GeneratorCheepCheep",
        0x18 to "SurfaceJumpingCheepCheep", 0x19 to "DisplayMessage",
        0x1A to "ClassicPiranhaPlant", 0x1B to "Football", 0x1C to "BulletBill",
        0x1D to "HoppingFlame", 0x1E to "Lakitu", 0x1F to "MagiKoopa", 0x20 to "Magic",
        0x24 to "Grinder", 0x26 to "Thwomp", 0x27 to "Thwimp",
        0x28 to "BouncingFootball", 0x29 to "KoopaKid", 0x2A to "GreenShell",
        0x2B to "SumoLightning", 0x2C to "YoshiEgg",
        0x2F to "PortableSpringboard", 0x30 to "ThrowingDryBones", 0x31 to "BonyBeetle",
        0x33 to "Podoboo", 0x34 to "LudwigFireball", 0x35 to "Yoshi",
        0x3D to "RipVanFish", 0x3E to "PSwitch", 0x3F to "ParachuteGoomba",
        0x44 to "TorpedoTed", 0x45 to "DirectionalCoins",
        0x46 to "DigginChuck", 0x47 to "SwimmingAndJumpingCheepCheep", 0x48 to "DigginChuckRock",
        0x49 to "ShiftingPipe", 0x4A to "GoalSphere", 0x4B to "PipeLakitu",
        0x4C to "ExplodingBlock", 0x4E to "LedgeMontyMole", 0x4F to "JumpingPiranhaPlant",
        0x51 to "Ninji", 0x52 to "MovingLedgeHole",
        0x54 to "ClimbingNetDoor", 0x57 to "VerticalCheckerboardPlatform",
        0x58 to "VerticalRockPlatform", 0x5F to "BrownChainedPlatform",
        0x60 to "FlatPalaceSwitch", 0x61 to "SkullRaft",
        0x6A to "CoinGameCloud", 0x6C to "RightWallSpringboard", 0x6E to "DinoRhino",
        0x6F to "DinoTorch", 0x70 to "Pokey", 0x71 to "RedCapeSuperKoopa",
        0x73 to "GroundSuperKoopa", 0x76 to "Star", 0x77 to "Feather", 0x79 to "VineHead",
        0x7A to "Fireworks", 0x7B to "GoalTape", 0x7C to "PrincessPeach", 0x80 to "Key",
        0x81 to "ChangingItem", 0x82 to "BonusGame", 0x83 to "LeftFlyingBlock",
        0x86 to "Wiggler", 0x87 to "LakituCloud", 0x88 to "WingedCage", 0x89 to "Layer3Smasher",
        0x8A to "Bird", 0x8B to "FireplaceSmoke", 0x8C to "SideExitAndFireplace",
        0x8D to "GhostHouseDoor", 0x8E to "WarpHole", 0x8F to "ScalePlatform",
        0x90 to "GreenGasBubble", 0x91 to "CharginChuck", 0x95 to "ClappinChuck",
        0x99 to "VolcanoLotus", 0x9A to "SumoBro", 0x9B to "HammerBro",
        0x9C to "HammerBroPlatform", 0x9D to "BubbleWithSprite", 0x9E to "BallNChain",
        0x9F to "BanzaiBill", 0xA0 to "ActivateBowserBattle", 0xA1 to "BowserBowlingBall",
        0xA2 to "MechaKoopa", 0xA3 to "GreyChainedPlatform", 0xA5 to "Sparky", 0xA7 to "IggyBall",
        0xA8 to "Blargg", 0xA9 to "Reznor", 0xAA to "Fishbone", 0xAB to "Rex",
        0xAC to "DownFirstWoodenSpike", 0xAD to "UpDownFirstWoodenSpike", 0xAE to "FishinBoo",
        0xB1 to "CreateEatBlock", 0xB2 to "FallingSpike", 0xB3 to "BowserStatueFire",
        0xB4 to "NonLineGuideGrinder", 0xB7 to "CarrotTopLiftUpperRight",
        0xB8 to "CarrotTopLiftUpperLeft", 0xB9 to "MessageBox", 0xBA to "TimedPlatform",
        0xBB to "MovingCastleStone", 0xBC to "BowserStatue", 0xBD to "SlidingNakedBlueKoopa",
        0xBE to "Swooper", 0xBF to "MegaMole", 0xC0 to "SinkingLavaPlatform",
        0xC1 to "WingedPlatform", 0xC2 to "Blurp", 0xC3 to "PorcuPuffer",
        0xC4 to "GreyFallingPlatform", 0xC5 to "BigBooBoss", 0xC6 to "Spotlight",
        0xC7 to "InvisibleMushroom", 0xC8 to "LightSwitch",
    )

    /** Nombre canónico del sprite [id], o "Sprite 0xXX" si no está catalogado. */
    fun nameOf(id: Int): String = names[id] ?: "Sprite 0x%02X".format(id)
}
