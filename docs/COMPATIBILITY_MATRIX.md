# Compatibilité (à compléter après tests serveur)

## Cible annoncée (code / API)
- Java: **17+**
- API compile: **Paper 1.20.6**
- Objectif runtime: **Paper / Purpur / Spigot 1.20.4 -> 1.21.x** (à valider)

## Matrice de test (à remplir avant vente)

| Plateforme | Version | Démarrage | Claim | GUI | Admin | PlaceholderAPI | Storage YAML | SQLite | MySQL | OK vente |
|---|---:|---|---|---|---|---|---|---|---|---|
| Paper | 1.20.4 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Paper | 1.20.6 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Paper | 1.21.x | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Purpur | 1.20.6 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Purpur | 1.21.x | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Spigot | 1.20.4+ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |

## Cas à tester (stabilité premium)
- spam clic sur le bouton claim (aucun double gain)
- `/ritual claim` spammé en macro
- relog pendant cooldown
- reboot serveur puis claim (état conservé)
- config invalide (material, slot, heure) -> warnings doctor lisibles
- commandes rewards invalides -> pas de crash, warning console + warning joueur
- MySQL indisponible au boot -> fallback YAML (si configuré)
