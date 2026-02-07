# CobbleMarket

A full-featured player marketplace (GTS) for Cobblemon servers. Buy, sell, and auction Pokemon and items with an intuitive GUI interface.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.90+
- Cobblemon 1.7.2+
- CobbleLib 1.0.0+
- Java 21+

## Features

### Pokemon Trading
- List Pokemon for sale with fixed prices
- Smart pricing formulas based on IVs, shininess, and rarity
- Minimum price tiers prevent underselling valuable Pokemon
- Full stat display in listing previews

### Item Trading
- Sell any Minecraft or Cobblemon items
- Configurable item blacklists
- Stack support for bulk sales

### Auction System
- Time-limited bidding on Pokemon and items
- Configurable minimum bid increments
- Automatic winner notification and delivery
- Auction duration limits (30 min - 7 days)

### Economy Integration
- Uses CobbleLib's Cobbletokens by default
- Configurable tax rates on sales
- Minimum and maximum price limits
- Multiple currency support

### Player Features
- Browse all active listings
- Search by Pokemon name or item
- View your active listings
- Reclaim expired listings
- Full transaction history

### Moderation Tools
- Timeout players from marketplace access
- Admin removal of inappropriate listings
- Configurable item/Pokemon blacklists
- Discord webhook notifications

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/market` | `cobblemarket.base` | Open the main marketplace menu |
| `/market pokemon` | `cobblemarket.base` | Browse Pokemon listings |
| `/market items` | `cobblemarket.base` | Browse item listings |
| `/market manage` | `cobblemarket.base` | View your active listings |
| `/market expired` | `cobblemarket.base` | Reclaim expired listings |
| `/market history` | `cobblemarket.base` | View transaction history |
| `/market search <query>` | `cobblemarket.base` | Search for listings |
| `/market reload` | `cobblemarket.reload` | Reload configuration |
| `/market admin timeout <player> <minutes>` | `cobblemarket.admin` | Timeout a player |
| `/market admin remove <listingId>` | `cobblemarket.admin` | Remove a listing |

**Aliases:** `/gts`, `/cobblemarket`

## Configuration

Config files are located at `config/cobblemarket/`

### config.json

```json
{
  "debug": false,
  "lang": "en",
  "commands": ["market", "gts", "cobblemarket"],
  "enablePokemonSales": true,
  "enableItemSales": true,
  "enableAuctions": true,
  "maxListingsPerPlayer": 8,
  "listingDurationHours": 72,
  "auctionMinDurationMinutes": 30,
  "auctionMaxDurationHours": 168,
  "taxRate": 0.10,
  "minimumPrice": 100,
  "maximumPrice": 10000000,
  "auctionMinBidIncrement": 100,
  "pokemonFormula": "100 + (level * 10) + (perfect_ivs * 500) + (shiny * 5000)",
  "broadcastNewListings": true,
  "broadcastSales": true
}
```

### Price Tiers

Minimum prices based on Pokemon attributes:

| Attribute | Minimum Price |
|-----------|---------------|
| 1 Perfect IV | 1,000 |
| 2 Perfect IVs | 2,000 |
| 3 Perfect IVs | 4,000 |
| 4 Perfect IVs | 8,000 |
| 5 Perfect IVs | 15,000 |
| 6 Perfect IVs | 30,000 |
| Hidden Ability | 5,000 |
| Shiny | 10,000 |
| Legendary | 25,000 |
| Mythical | 50,000 |

### Discord Integration

```json
{
  "discord": {
    "enabled": false,
    "webhookUrl": "",
    "notifyNewListings": true,
    "notifySales": true,
    "notifyAuctionEnd": true
  }
}
```

## Data Storage

- Active listings: `config/cobblemarket/listings/`
- Expired listings: `config/cobblemarket/expired/`
- Transaction history: `config/cobblemarket/history/`
- Language files: `config/cobblemarket/lang/`

## Building

```bash
./gradlew build
```

Output: `build/libs/cobblemarket-1.0.0.jar`

## License

All rights reserved.
