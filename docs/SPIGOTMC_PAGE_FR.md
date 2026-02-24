# Texte de page de vente SpigotMC (FR)

## Titre
**RitualPulse | Daily Rewards avancés, Streak, Jokers, Milestones, Lucky Hour, GUI, YAML/SQLite/MySQL**

## Intro (accroche)
RitualPulse est un plugin de récompenses journalières orienté **rétention** pour serveurs Minecraft.  
Ce n'est pas juste un `/daily` basique: tu peux créer de vraies mécaniques de fidélisation avec **streak**, **jokers anti-casse**, **bonus comeback**, **milestones**, **lucky hour** et **bonus VIP**.

## Ce que ça apporte (use case rétention)
- pousse les joueurs à revenir chaque jour
- récompense les joueurs réguliers sans casser l'économie
- permet des events horaires (lucky hour)
- donne des objectifs long terme (milestones 7/14/30+)
- s'intègre facilement avec économie, crates, ranks via commandes console

## Features principales
- ✅ Daily rewards par cycle configurable (jour 1..N)
- ✅ Streak + deadline de streak indépendante du cooldown
- ✅ Freeze token / joker pour sauver une streak ratée
- ✅ Bonus comeback après rupture de streak
- ✅ Lucky hour (fenêtre horaire configurable)
- ✅ Milestones (7j / 14j / 30j / custom)
- ✅ Bonus VIP par permission
- ✅ GUI configurable + claim par commande
- ✅ PlaceholderAPI (optionnel)
- ✅ YAML / SQLite / MySQL
- ✅ Doctor de config (`/ritual admin doctor`)
- ✅ Logs lisibles pour commandes reward cassées

## Focus stabilité / premium
- protection anti double-claim (spam click / spam commande)
- persistance d'état avant exécution des rewards (anti duplication en cas de crash/reboot)
- warnings clairs si config invalide
- fallback YAML si init SQL échoue

## Commandes
- `/ritual`
- `/ritual claim`
- `/ritual reload`
- `/ritual admin doctor`
- `/ritual admin inspect <player>`
- `/ritual admin reset <player>`
- `/ritual admin givefreeze <player> <amount>`
- `/ritual admin setstreak <player> <value>`

## Permissions
- `ritualpulse.use`
- `ritualpulse.admin`
- `ritualpulse.bypasscooldown`

## Compatibilité (à afficher après tes tests réels)
- Java 17+
- Paper / Purpur / Spigot: **1.20.4 -> 1.21.x** *(après validation de ta matrice de tests)*

## Dépendances optionnelles
- PlaceholderAPI (si tu veux des placeholders dans les commandes/messages)

## Exemples d'intégration
- EssentialsX / CMI (économie)
- Crates plugins (clés)
- LuckPerms (permissions temporaires / bonus)

## Support
- aide config / setup via Discord
- si tu remontes un bug avec config + logs + version Paper/Purpur, fix plus rapide

## Changelog (mini)
Voir `docs/CHANGELOG.md`

## Roadmap
Voir `docs/ROADMAP.md`
