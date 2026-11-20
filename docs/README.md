# Game mechanics and development reference

This is the developer reference for implemented game behavior. **It contains spoilers for
all secret routes, unlocks, bosses and endings.** The player-facing overview stays in
[README.md](../README.md); build/release entry points remain in
[DEVELOPING.md](../DEVELOPING.md) and agent conventions in [AGENTS.md](../AGENTS.md).

Start with [architecture](architecture.md) and [campaign](campaign.md), then follow the
relevant topic below. Pages explain behavior, implementation ownership, cross-system
interactions and existing checks. Source links are repository-relative and use class
names rather than fragile line numbers.

## Start here

| Topic | What to find |
| --- | --- |
| [Architecture and runtime](architecture.md) | Ownership, simulation, coordinates and safe mutation |
| [Campaign floors, encounters and endings](campaign.md) | Campaign route, encounters and endings |
| [Controls and interaction](controls.md) | Keyboard, mouse, pad and interaction priority |
| [Home, title screen and options](hub-and-menus.md) | Title, home spots, family selection and options |
| [Run screens, camera, HUD and presentation](screens-and-interface.md) | States, camera, HUD, maps and presentation |

## Combat and equipment

| Topic | What to find |
| --- | --- |
| [Player movement, health and heroes](player-and-heroes.md) | Hero traits, movement, roll, health and companions |
| [Combat, projectiles and damage](combat.md) | Projectiles, damage, blanks and death lifecycle |
| [Weapons, ammo and upgrades](weapons.md) | Firing families, ammo, upgrades and growing guns |
| [Guns](gun-reference.md) | Complete gun stat tables |
| [Items, active slots and companions](items.md) | Passive effects, active slots and companions |
| [Items](item-reference.md) | Complete item tables and cooldowns |
| [Inventory synergies](synergies.md) | Recipes, effects and restoration |
| [Fire, acid, gas, frost and control effects](status-effects.md) | Fire, acid, gas, frost, stun and petrification |
| [Loot pools, chests, drops and junk](loot-and-chests.md) | Pools, tiers, keys, drops and Junk Knight |
| [Coins, Baa-llo and selling loot](economy-and-shops.md) | Coin arithmetic, stock, prices, grudge and selling |

## World and game modes

| Topic | What to find |
| --- | --- |
| [Room generation, navigation, cover and pits](rooms-and-cover.md) | Map generation, navigation, collision, cover and pits |
| [Main-road stages and environmental mechanics](main-stages.md) | Village/town/city, tower elevators, clouds and birds |
| [Regular enemies and AI](enemies.md) | AI, regional populations, variants and mimics |
| [Boss roster and fight lifecycle](bosses.md) | Full roster, eligibility, arenas and fight lifecycle |
| [Difficulty, hard mode and encounter scaling](difficulty.md) | Scaling, hard mode, crowding and boss clocks |
| [Slums entrance, acid gang and trap rooms](slums.md) | Secret entrance, acid gang, traps and bosses |
| [Undercity and the False King ending](undercity.md) | Marked critters, False King and continuation |
| [Deep Sea detour and underwater rules](deep-sea.md) | Dive/surface, water rules, creatures and bosses |
| [Moon, veterans and Grandfluff](moon.md) | Meteor showers, Moon folk/veterans and Grandfluff |
| [Endless waves, rewards and travelling shops](endless.md) | Wave loop, boss order, ammo aid and travelling stalls |
| [Dreams, daily runs and boss rush](dreams.md) | Dreams, date seeds, rules, rush and records |
| [Under the Bed residents, bosses and completion](under-the-bed.md) | Unlock, short trip, residents and hero-specific bosses |
| [Family rocket, Moon ride and console game](rocket-and-console.md) | Family rocket, ride and Egor’s score chase |

## Persistence and development

| Topic | What to find |
| --- | --- |
| [Run saves, restoration and compatibility](saving.md) | Snapshot contents, restore order and compatibility |
| [Unlocks, achievements and the Flufficon](progression.md) | Unlocks, records, Flufficon and achievements |
| [Rendering, depth, lighting and camera geometry](rendering.md) | Draw passes, depth, lighting, materials and scale |
| [Generated art, sound and music](assets.md) | Generated art/audio formats, scripts and registration |
| [Dev builds, flags and recording](debugging.md) | Dev profile, full flag reference and recording |
| [Balancing, bot runs and test evidence](balancing.md) | Bot sampling, fairness tests and historical evidence |

## Keeping the reference accurate

Update the relevant topic in the same change as a mechanic. Put rules in their topic
page and numbers/catalog entries in the gun/item references; link across topics instead
of maintaining another full copy in AGENTS or DEVELOPING. Keep source and test links
current. Register a new topic here when it merits a separate page.

The implementation and its tests settle behavior when prose disagrees; fix the prose
rather than preserving stale rules. Label historical balance samples with their date,
loadout and limitations. Proposed/unimplemented mechanics belong in
[ROADMAP.md](../ROADMAP.md), not as present-tense rules here. Release history belongs in
[CHANGELOG.md](../CHANGELOG.md).

The PNG/GIF files already in this folder are README media, not generated mechanics docs.
