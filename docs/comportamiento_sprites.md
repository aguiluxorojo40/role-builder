# Comportamiento (tweaker) de los sprites de SMW

Los seis bytes de propiedades por id de sprite (0x00–0xC8), leídos de la ROM real
(`InitializeNormalSpriteRAMTables_PropertyTables`, $07:F7A0). Son el dato fiel; los
nombres de sprite salen de `SmwSpriteNames`. `[B]` = sprite grande reconstruido ·
`[s]` = roster pequeño · `[ ]` = solo comportamiento (sin gráfico reconstruido).

## Papel de cada byte (de `SmwSpriteBehavior`)

| Byte | RAM | Papel |
|------|-----|-------|
| 1656 | $7E:1656 | banderas varias (interacción con jugador/objetos/estrella); bits 0-3 = **object-clip** (hitbox con objetos) |
| 1662 | $7E:1662 | bits 0-5 = **sprite-clip** (hitbox con jugador/sprites); resto banderas |
| 166E | $7E:166E | banderas de proceso (fuera de pantalla, dirección inicial…); nibble bajo → página/paleta de tesela (`spr_table15f6`) |
| 167A | $7E:167A | banderas de interacción (con jugador, capa, otros sprites) |
| 1686 | $7E:1686 | más banderas (gravedad, no interacción con objetos…) |
| 190F | $7E:190F | banderas extra (inmunidades, comportamiento especial) |

## Tabla por sprite

| id | Sprite | 1656 | 1662 | 166E | 167A | 1686 | 190F | obj-clip | spr-clip |
|----|--------|------|------|------|------|------|------|----------|----------|
| [s] 0x00 | GreenKoopa | 70 | 00 | 0A | 00 | 00 | 00 | 0 | 0 |
| [s] 0x01 | RedKoopa | 70 | 00 | 08 | 00 | 00 | 00 | 0 | 0 |
| [s] 0x02 | BlueKoopa | 70 | 00 | 06 | 00 | 00 | 00 | 0 | 0 |
| [s] 0x03 | YellowKoopa | 70 | 00 | 04 | 00 | 00 | 00 | 0 | 0 |
| [ ] 0x04 | GreenKoopaNoShell | 10 | 40 | 0A | 00 | 02 | A0 | 0 | 0 |
| [s] 0x05 | RedKoopaNoShell | 10 | 40 | 08 | 00 | 02 | A0 | 0 | 0 |
| [ ] 0x06 | BlueKoopaNoShell | 10 | 40 | 06 | 00 | 02 | A0 | 0 | 0 |
| [ ] 0x07 | YellowKoopaNoShell | 10 | 40 | 04 | 00 | 02 | A0 | 0 | 0 |
| [B] 0x08 | GreenParakoopa | 10 | 40 | 0A | 00 | 42 | B0 | 0 | 0 |
| [ ] 0x09 | RedParakoopa | 10 | 40 | 0A | 00 | 52 | B0 | 0 | 0 |
| [ ] 0x0A | GreenFlyingParakoopa | 10 | 40 | 08 | 00 | 52 | B0 | 0 | 0 |
| [ ] 0x0B | BobOmb | 10 | 40 | 08 | 00 | 52 | B0 | 0 | 0 |
| [ ] 0x0C | BulletBillGenerator | 10 | 40 | 04 | 00 | 52 | A0 | 0 | 0 |
| [ ] 0x0D | Sprite 0x0D | 10 | 00 | 17 | 18 | 00 | 80 | 0 | 0 |
| [ ] 0x0E | Keyhole | 00 | 0A | 32 | 02 | 09 | 44 | 0 | 10 |
| [s] 0x0F | Goomba | 10 | 00 | 04 | 18 | 00 | 80 | 0 | 0 |
| [B] 0x10 | ParaGoomba | 10 | 00 | 04 | 00 | 40 | 80 | 0 | 0 |
| [s] 0x11 | BuzzyBeetle | 10 | 00 | 1D | 00 | 00 | 80 | 0 | 0 |
| [ ] 0x12 | Sprite 0x12 | 14 | 08 | 3D | 81 | 01 | 20 | 4 | 8 |
| [ ] 0x13 | KoopaKidBossFight | 00 | 00 | 09 | 00 | 00 | 00 | 0 | 0 |
| [ ] 0x14 | SpinyEgg | 00 | 00 | 09 | 01 | 00 | 00 | 0 | 0 |
| [ ] 0x15 | Sprite 0x15 | 00 | 00 | 45 | 99 | 10 | 00 | 0 | 0 |
| [ ] 0x16 | VerticalCheepCheep | 00 | 00 | 45 | 99 | 10 | 00 | 0 | 0 |
| [ ] 0x17 | GeneratorCheepCheep | 10 | 80 | 85 | 00 | 90 | 00 | 0 | 0 |
| [ ] 0x18 | SurfaceJumpingCheepCheep | 10 | 80 | 85 | 00 | 90 | 00 | 0 | 0 |
| [ ] 0x19 | DisplayMessage | 11 | 81 | 0B | 01 | 01 | 20 | 1 | 1 |
| [s] 0x1A | ClassicPiranhaPlant | 81 | 01 | 08 | 00 | 10 | 20 | 1 | 1 |
| [ ] 0x1B | Football | 10 | 80 | 01 | 00 | 10 | 00 | 0 | 0 |
| [B] 0x1C | BulletBill | 10 | 80 | 12 | 00 | 90 | 00 | 0 | 0 |
| [ ] 0x1D | HoppingFlame | 80 | 00 | 15 | 00 | 00 | 00 | 0 | 0 |
| [ ] 0x1E | Lakitu | 11 | 81 | 09 | 00 | 11 | 60 | 1 | 1 |
| [B] 0x1F | MagiKoopa | 11 | 81 | 4F | 02 | 01 | 20 | 1 | 1 |
| [ ] 0x20 | Magic | 82 | 00 | 1C | 00 | 01 | 04 | 2 | 0 |
| [ ] 0x21 | Sprite 0x21 | 00 | 00 | 24 | C2 | 08 | 04 | 0 | 0 |
| [ ] 0x22 | Sprite 0x22 | 13 | 81 | 0B | 00 | 00 | 20 | 3 | 1 |
| [ ] 0x23 | Sprite 0x23 | 13 | 81 | 09 | 00 | 00 | 20 | 3 | 1 |
| [ ] 0x24 | Grinder | 13 | 81 | 0B | 00 | 00 | 20 | 3 | 1 |
| [ ] 0x25 | Sprite 0x25 | 13 | 81 | 09 | 00 | 00 | 20 | 3 | 1 |
| [B] 0x26 | Thwomp | 01 | 06 | 33 | 01 | 01 | 24 | 1 | 6 |
| [ ] 0x27 | Thwimp | 00 | 00 | 33 | 01 | 01 | 04 | 0 | 0 |
| [ ] 0x28 | BouncingFootball | 00 | 07 | FD | 01 | 19 | 00 | 0 | 7 |
| [s] 0x29 | KoopaKid | 00 | 06 | 2B | 83 | 80 | 44 | 0 | 6 |
| [s] 0x2A | GreenShell | 81 | 01 | 08 | 00 | 00 | 20 | 1 | 1 |
| [ ] 0x2B | SumoLightning | 00 | 00 | 35 | 00 | 39 | 04 | 0 | 0 |
| [s] 0x2C | YoshiEgg | 00 | 00 | 3B | 9A | 09 | 44 | 0 | 0 |
| [ ] 0x2D | Sprite 0x2D | 00 | 00 | 3A | 1E | 09 | C4 | 0 | 0 |
| [ ] 0x2E | Sprite 0x2E | 00 | 00 | 19 | 01 | 10 | 00 | 0 | 0 |
| [ ] 0x2F | PortableSpringboard | 00 | 00 | 3A | BE | 0A | C4 | 0 | 0 |
| [ ] 0x30 | ThrowingDryBones | 00 | 37 | 13 | 81 | 09 | 24 | 0 | 55 |
| [B] 0x31 | BonyBeetle | 00 | 00 | 13 | 81 | 09 | 24 | 0 | 0 |
| [ ] 0x32 | Sprite 0x32 | 00 | 37 | 13 | 81 | 09 | 24 | 0 | 55 |
| [ ] 0x33 | Podoboo | 00 | 00 | 34 | 02 | 99 | 04 | 0 | 0 |
| [ ] 0x34 | LudwigFireball | 00 | 00 | 39 | 18 | 18 | 04 | 0 | 0 |
| [ ] 0x35 | Yoshi | 05 | 09 | 2A | 87 | 29 | 46 | 5 | 9 |
| [ ] 0x36 | Sprite 0x36 | 80 | 01 | 15 | 02 | 08 | 04 | 0 | 1 |
| [ ] 0x37 | Sprite 0x37 | 00 | 00 | F3 | 01 | 19 | 04 | 0 | 0 |
| [ ] 0x38 | Sprite 0x38 | 00 | 00 | FD | 01 | 19 | 04 | 0 | 0 |
| [ ] 0x39 | Sprite 0x39 | 00 | 00 | FD | 01 | 19 | 04 | 0 | 0 |
| [ ] 0x3A | Sprite 0x3A | 07 | 0E | 37 | 01 | 11 | 04 | 7 | 14 |
| [ ] 0x3B | Sprite 0x3B | 07 | 0E | 37 | 01 | 11 | 04 | 7 | 14 |
| [ ] 0x3C | Sprite 0x3C | 07 | 0E | 37 | 01 | 15 | 04 | 7 | 14 |
| [ ] 0x3D | RipVanFish | 00 | 00 | C7 | 00 | 10 | 00 | 0 | 0 |
| [s] 0x3E | PSwitch | 00 | 00 | 30 | 3E | 0A | C4 | 0 | 0 |
| [ ] 0x3F | ParachuteGoomba | 30 | 00 | 05 | 01 | 40 | 00 | 0 | 0 |
| [ ] 0x40 | Sprite 0x40 | 30 | 00 | 15 | 01 | 40 | 00 | 0 | 0 |
| [ ] 0x41 | Sprite 0x41 | 00 | 0F | 37 | 82 | 8D | 05 | 0 | 15 |
| [ ] 0x42 | Sprite 0x42 | 00 | 0F | 37 | 82 | 8D | 05 | 0 | 15 |
| [ ] 0x43 | Sprite 0x43 | 00 | 10 | 37 | 82 | 8D | 05 | 0 | 16 |
| [ ] 0x44 | TorpedoTed | 00 | 14 | 33 | 01 | 11 | 04 | 0 | 20 |
| [ ] 0x45 | DirectionalCoins | 08 | 00 | 30 | 02 | 18 | 44 | 8 | 0 |
| [ ] 0x46 | DigginChuck | 00 | 0D | 8B | 81 | 11 | 48 | 0 | 13 |
| [ ] 0x47 | SwimmingAndJumpingCheepCheep | 10 | 80 | 85 | 00 | 80 | 00 | 0 | 0 |
| [ ] 0x48 | DigginChuckRock | 00 | 00 | 1D | 00 | 00 | 00 | 0 | 0 |
| [ ] 0x49 | ShiftingPipe | 00 | 1D | 3B | A2 | 29 | 40 | 0 | 29 |
| [ ] 0x4A | GoalSphere | 00 | 00 | 3B | 82 | 29 | 40 | 0 | 0 |
| [s] 0x4B | PipeLakitu | 10 | 80 | 09 | 01 | 10 | 40 | 0 | 0 |
| [ ] 0x4C | ExplodingBlock | 00 | 80 | 34 | 00 | 10 | 04 | 0 | 0 |
| [s] 0x4D | Sprite 0x4D | 10 | 80 | 01 | 00 | 10 | 00 | 0 | 0 |
| [s] 0x4E | LedgeMontyMole | 10 | 80 | 01 | 00 | 10 | 00 | 0 | 0 |
| [s] 0x4F | JumpingPiranhaPlant | 8C | 00 | 08 | 00 | 00 | 00 | 12 | 0 |
| [ ] 0x50 | Sprite 0x50 | 8C | 00 | 08 | 00 | 00 | 00 | 12 | 0 |
| [ ] 0x51 | Ninji | 10 | 80 | 09 | 00 | 10 | 00 | 0 | 0 |
| [ ] 0x52 | MovingLedgeHole | 00 | 02 | 20 | A2 | 29 | 64 | 0 | 2 |
| [ ] 0x53 | Sprite 0x53 | 00 | 0C | 30 | 08 | 20 | C4 | 0 | 12 |
| [ ] 0x54 | ClimbingNetDoor | 00 | 03 | 20 | 02 | 29 | 64 | 0 | 3 |
| [ ] 0x55 | Sprite 0x55 | 00 | 05 | E3 | A2 | A9 | 45 | 0 | 5 |
| [ ] 0x56 | Sprite 0x56 | 01 | 04 | E3 | A2 | A9 | 65 | 1 | 4 |
| [ ] 0x57 | VerticalCheckerboardPlatform | 00 | 05 | E3 | A2 | A9 | 45 | 0 | 5 |
| [ ] 0x58 | VerticalRockPlatform | 01 | 04 | E3 | A2 | A9 | 65 | 1 | 4 |
| [ ] 0x59 | Sprite 0x59 | 01 | 00 | E3 | A2 | A9 | 45 | 1 | 0 |
| [ ] 0x5A | Sprite 0x5A | 01 | 00 | E3 | A2 | A9 | 45 | 1 | 0 |
| [ ] 0x5B | Sprite 0x5B | 0B | 04 | E1 | A2 | A9 | 45 | 11 | 4 |
| [ ] 0x5C | Sprite 0x5C | 0B | 05 | E1 | A2 | A9 | 45 | 11 | 5 |
| [ ] 0x5D | Sprite 0x5D | 0B | 04 | EB | A2 | A9 | 65 | 11 | 4 |
| [ ] 0x5E | Sprite 0x5E | 0B | 05 | EB | A2 | A9 | 65 | 11 | 5 |
| [ ] 0x5F | BrownChainedPlatform | 00 | 00 | E3 | A2 | A9 | 45 | 0 | 0 |
| [ ] 0x60 | FlatPalaceSwitch | 00 | 1D | E3 | A2 | 29 | 45 | 0 | 29 |
| [ ] 0x61 | SkullRaft | 00 | 0C | E3 | A2 | 29 | 45 | 0 | 12 |
| [ ] 0x62 | Sprite 0x62 | 00 | 04 | E1 | A2 | 3D | 45 | 0 | 4 |
| [ ] 0x63 | Sprite 0x63 | 00 | 04 | E1 | A2 | 3D | 45 | 0 | 4 |
| [ ] 0x64 | Sprite 0x64 | 00 | 12 | A3 | A2 | 3D | 45 | 0 | 18 |
| [ ] 0x65 | Sprite 0x65 | 00 | 20 | A3 | 22 | 3D | 45 | 0 | 32 |
| [ ] 0x66 | Sprite 0x66 | 00 | 21 | A3 | 22 | 3D | 45 | 0 | 33 |
| [ ] 0x67 | Sprite 0x67 | 00 | 2C | A3 | 22 | 3D | 05 | 0 | 44 |
| [ ] 0x68 | Sprite 0x68 | 00 | 34 | A3 | 22 | 3D | 05 | 0 | 52 |
| [ ] 0x69 | Sprite 0x69 | 00 | 04 | E3 | A2 | 29 | 44 | 0 | 4 |
| [ ] 0x6A | CoinGameCloud | 00 | 04 | F0 | A2 | 19 | 44 | 0 | 4 |
| [ ] 0x6B | Sprite 0x6B | 00 | 04 | E3 | A2 | 29 | 44 | 0 | 4 |
| [ ] 0x6C | RightWallSpringboard | 00 | 04 | F3 | A2 | 29 | 44 | 0 | 4 |
| [ ] 0x6D | Sprite 0x6D | 10 | 0C | 3F | E2 | 59 | 46 | 0 | 12 |
| [ ] 0x6E | DinoRhino | 19 | 16 | 3F | 01 | 59 | 00 | 9 | 22 |
| [ ] 0x6F | DinoTorch | 30 | 00 | 0F | 01 | 18 | 00 | 0 | 0 |
| [B] 0x70 | Pokey | 0A | 17 | 35 | 01 | 18 | 00 | 10 | 23 |
| [ ] 0x71 | RedCapeSuperKoopa | 10 | 80 | 0B | 01 | 10 | 10 | 0 | 0 |
| [ ] 0x72 | Sprite 0x72 | 10 | 80 | 09 | 01 | 10 | 10 | 0 | 0 |
| [ ] 0x73 | GroundSuperKoopa | 30 | 00 | 07 | 01 | 50 | 10 | 0 | 0 |
| [ ] 0x74 | Sprite 0x74 | 00 | 00 | 08 | C2 | 28 | 40 | 0 | 0 |
| [ ] 0x75 | Sprite 0x75 | 00 | 00 | 0A | C2 | 28 | 40 | 0 | 0 |
| [ ] 0x76 | Star | 00 | 00 | 20 | C2 | 28 | 40 | 0 | 0 |
| [ ] 0x77 | Feather | 00 | 00 | 24 | C2 | 28 | 40 | 0 | 0 |
| [ ] 0x78 | Sprite 0x78 | 00 | 00 | 0A | C2 | 08 | 40 | 0 | 0 |
| [ ] 0x79 | VineHead | 00 | 00 | 3A | 82 | 29 | 40 | 0 | 0 |
| [ ] 0x7A | Fireworks | 00 | 00 | 3A | 82 | 29 | 40 | 0 | 0 |
| [ ] 0x7B | GoalTape | 00 | 1E | 20 | A2 | 39 | 42 | 0 | 30 |
| [ ] 0x7C | PrincessPeach | 01 | 35 | 20 | A2 | 39 | 42 | 1 | 53 |
| [ ] 0x7D | Sprite 0x7D | 00 | 00 | 21 | 9A | 29 | 40 | 0 | 0 |
| [ ] 0x7E | Sprite 0x7E | 00 | 00 | 28 | 80 | 28 | 40 | 0 | 0 |
| [ ] 0x7F | Sprite 0x7F | 00 | 00 | 20 | 82 | 28 | 40 | 0 | 0 |
| [ ] 0x80 | Key | 00 | 0C | 20 | 3E | 3A | C0 | 0 | 12 |
| [ ] 0x81 | ChangingItem | 00 | 00 | 00 | C2 | 28 | 40 | 0 | 0 |
| [ ] 0x82 | BonusGame | 00 | 00 | 20 | 82 | 29 | 40 | 0 | 0 |
| [ ] 0x83 | LeftFlyingBlock | 00 | 0C | 20 | 82 | 31 | 40 | 0 | 12 |
| [ ] 0x84 | Sprite 0x84 | 00 | 0C | 20 | 82 | 31 | 40 | 0 | 12 |
| [ ] 0x85 | Sprite 0x85 | 00 | 00 | 20 | 92 | 29 | 00 | 0 | 0 |
| [ ] 0x86 | Wiggler | 00 | 00 | F5 | 80 | 00 | 00 | 0 | 0 |
| [ ] 0x87 | LakituCloud | 00 | 3A | 20 | 82 | 29 | 40 | 0 | 58 |
| [ ] 0x88 | WingedCage | 00 | 08 | 20 | 82 | 29 | 40 | 0 | 8 |
| [ ] 0x89 | Layer3Smasher | 00 | 08 | 20 | 82 | 29 | 40 | 0 | 8 |
| [ ] 0x8A | Bird | 00 | 00 | 20 | 02 | 29 | 40 | 0 | 0 |
| [ ] 0x8B | FireplaceSmoke | 00 | 00 | 20 | 02 | 29 | 40 | 0 | 0 |
| [ ] 0x8C | SideExitAndFireplace | 00 | 00 | 20 | 02 | 29 | 40 | 0 | 0 |
| [ ] 0x8D | GhostHouseDoor | 00 | 00 | 20 | 02 | 29 | 40 | 0 | 0 |
| [ ] 0x8E | WarpHole | 00 | 1C | 30 | A2 | 29 | 40 | 0 | 28 |
| [ ] 0x8F | ScalePlatform | 00 | 08 | 3B | A2 | 29 | 01 | 0 | 8 |
| [ ] 0x90 | GreenGasBubble | 00 | 38 | F3 | 01 | 11 | 00 | 0 | 56 |
| [B] 0x91 | CharginChuck | 00 | 0D | 0B | F9 | 11 | 48 | 0 | 13 |
| [ ] 0x92 | Sprite 0x92 | 00 | 0D | 0B | F9 | 11 | 48 | 0 | 13 |
| [ ] 0x93 | Sprite 0x93 | 00 | 0D | 0B | F9 | 11 | 48 | 0 | 13 |
| [ ] 0x94 | Sprite 0x94 | 00 | 0D | 0B | F9 | 11 | 48 | 0 | 13 |
| [ ] 0x95 | ClappinChuck | 00 | 0D | 0B | F9 | 11 | 48 | 0 | 13 |
| [ ] 0x96 | Sprite 0x96 | 00 | 0D | 0B | F9 | 11 | 48 | 0 | 13 |
| [ ] 0x97 | Sprite 0x97 | 00 | 0D | 0B | F9 | 11 | 48 | 0 | 13 |
| [ ] 0x98 | Sprite 0x98 | 00 | 0D | 0B | F9 | 11 | 48 | 0 | 13 |
| [ ] 0x99 | VolcanoLotus | 80 | 00 | 9B | 01 | 10 | 00 | 0 | 0 |
| [ ] 0x9A | SumoBro | 00 | 0D | 93 | 01 | 11 | 40 | 0 | 13 |
| [ ] 0x9B | HammerBro | 10 | 80 | 00 | 01 | 01 | 40 | 0 | 0 |
| [ ] 0x9C | HammerBroPlatform | 00 | 1D | 30 | A2 | 39 | 40 | 0 | 29 |
| [ ] 0x9D | BubbleWithSprite | 00 | 00 | 31 | 81 | 10 | 00 | 0 | 0 |
| [ ] 0x9E | BallNChain | 00 | 00 | 31 | 00 | 19 | 04 | 0 | 0 |
| [B] 0x9F | BanzaiBill | 10 | B6 | 31 | 01 | 19 | 04 | 0 | 54 |
| [ ] 0xA0 | ActivateBowserBattle | 00 | 24 | FB | 80 | 19 | 40 | 0 | 36 |
| [ ] 0xA1 | BowserBowlingBall | 00 | 23 | FB | 00 | 19 | 40 | 0 | 35 |
| [ ] 0xA2 | MechaKoopa | 10 | 3B | BB | 19 | 01 | 00 | 0 | 59 |
| [ ] 0xA3 | GreyChainedPlatform | 00 | 1F | E3 | A2 | 29 | 41 | 0 | 31 |
| [ ] 0xA4 | Sprite 0xA4 | 00 | 22 | F3 | 01 | 98 | 00 | 0 | 34 |
| [ ] 0xA5 | Sparky | 00 | 00 | 35 | 01 | 14 | 00 | 0 | 0 |
| [ ] 0xA6 | Sprite 0xA6 | 00 | 27 | 35 | 01 | 14 | 00 | 0 | 39 |
| [ ] 0xA7 | IggyBall | 00 | 00 | 39 | 00 | 10 | 00 | 0 | 0 |
| [B] 0xA8 | Blargg | 00 | 00 | 35 | 00 | 18 | 00 | 0 | 0 |
| [ ] 0xA9 | Reznor | 00 | 28 | 35 | 81 | 18 | 40 | 0 | 40 |
| [ ] 0xAA | Fishbone | 00 | 00 | 7D | 01 | 18 | 00 | 0 | 0 |
| [ ] 0xAB | Rex | 00 | 2A | 07 | 81 | 00 | 00 | 0 | 42 |
| [ ] 0xAC | DownFirstWoodenSpike | 00 | 2B | 37 | 81 | 19 | 40 | 0 | 43 |
| [ ] 0xAD | UpDownFirstWoodenSpike | 00 | 2B | 37 | 81 | 19 | 40 | 0 | 43 |
| [ ] 0xAE | FishinBoo | 00 | 00 | 3D | 00 | 19 | 00 | 0 | 0 |
| [ ] 0xAF | Sprite 0xAF | 00 | 00 | 3F | 01 | 19 | 00 | 0 | 0 |
| [ ] 0xB0 | Sprite 0xB0 | 00 | 00 | 3F | 01 | 19 | 00 | 0 | 0 |
| [ ] 0xB1 | CreateEatBlock | 0D | 0C | 30 | A2 | 1D | 40 | 13 | 12 |
| [ ] 0xB2 | FallingSpike | 00 | 00 | 31 | 00 | 1D | 00 | 0 | 0 |
| [ ] 0xB3 | BowserStatueFire | 00 | 2D | 31 | 00 | 19 | 00 | 0 | 45 |
| [ ] 0xB4 | NonLineGuideGrinder | 00 | 00 | 31 | 00 | 19 | 00 | 0 | 0 |
| [ ] 0xB5 | Sprite 0xB5 | 00 | 00 | 04 | 00 | 18 | 00 | 0 | 0 |
| [ ] 0xB6 | Sprite 0xB6 | 00 | 00 | 35 | 00 | 18 | 00 | 0 | 0 |
| [ ] 0xB7 | CarrotTopLiftUpperRight | 00 | 2E | 3B | A2 | 19 | 41 | 0 | 46 |
| [ ] 0xB8 | CarrotTopLiftUpperLeft | 00 | 2E | 3B | A2 | 19 | 41 | 0 | 46 |
| [ ] 0xB9 | MessageBox | 00 | 0C | 36 | A2 | 19 | 40 | 0 | 12 |
| [ ] 0xBA | TimedPlatform | 00 | 1D | 7B | A2 | 1D | 41 | 0 | 29 |
| [ ] 0xBB | MovingCastleStone | 00 | 2F | 3B | A2 | 19 | 40 | 0 | 47 |
| [ ] 0xBC | BowserStatue | 00 | 0C | 33 | A0 | 18 | 00 | 0 | 12 |
| [ ] 0xBD | SlidingNakedBlueKoopa | 70 | 00 | 06 | 01 | 00 | 00 | 0 | 0 |
| [ ] 0xBE | Swooper | 10 | 80 | 0B | 01 | 10 | 00 | 0 | 0 |
| [B] 0xBF | MegaMole | 0E | 30 | 11 | A1 | 00 | 20 | 14 | 48 |
| [ ] 0xC0 | SinkingLavaPlatform | 00 | 32 | F5 | A2 | 99 | 47 | 0 | 50 |
| [ ] 0xC1 | WingedPlatform | 00 | 31 | F5 | A2 | 99 | 45 | 0 | 49 |
| [ ] 0xC2 | Blurp | 00 | 00 | CB | 01 | 10 | 00 | 0 | 0 |
| [ ] 0xC3 | PorcuPuffer | 00 | 00 | CD | 01 | 90 | 00 | 0 | 0 |
| [ ] 0xC4 | GreyFallingPlatform | 00 | 33 | F3 | A2 | A9 | 41 | 0 | 51 |
| [ ] 0xC5 | BigBooBoss | 00 | 07 | 3F | A3 | B9 | 41 | 0 | 7 |
| [ ] 0xC6 | Spotlight | FF | FF | FF | FF | FF | FF | 15 | 63 |
| [ ] 0xC7 | InvisibleMushroom | 00 | 00 | 20 | 82 | 39 | 40 | 0 | 0 |
| [ ] 0xC8 | LightSwitch | 00 | 0C | 38 | A2 | 19 | 40 | 0 | 12 |
