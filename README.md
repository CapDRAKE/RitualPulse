# RitualPulse v1.0

Plugin Paper/Spigot orienté rétention:
- daily rewards
- streak
- freeze token (joker)
- comeback bonus
- lucky hour
- milestones
- GUI
- YAML / SQLite / MySQL
- hook PlaceholderAPI (optionnel)
- doctor de config (`/ritual admin doctor`)
- logs lisibles si une commande reward casse

## compilation (Eclipse + Maven)

### prerequis
- JDK 17 (recommandé)
- Eclipse avec support Maven (M2E)
- Internet au premier import (pour télécharger les deps Maven)

### etapes
1. **Dézippe** le projet.
2. Dans Eclipse : **File > Import > Maven > Existing Maven Projects**.
3. Sélectionne le dossier `RitualPulse-v1.1` (ou le nom du dossier que tu as extrait).
4. Clique sur **Finish**.
5. Si besoin : clic droit projet > **Maven > Update Project**.
6. Pour compiler : clic droit projet > **Run As > Maven build...**
   - Goals: `clean package`
7. Le jar sera généré dans : `target/RitualPulse.jar`

## install
- Dépose `RitualPulse.jar` dans `plugins/`
- Démarre le serveur
- Configure `plugins/RitualPulse/config.yml`
- `/ritual reload`
- `/ritual admin doctor` (vérifie les warnings avant prod)

## commandes
- `/ritual`
- `/ritual claim`
- `/ritual reload`
- `/ritual admin doctor`
- `/ritual admin reset <player>`
- `/ritual admin givefreeze <player> <amount>`
- `/ritual admin setstreak <player> <value>`
- `/ritual admin inspect <player>`

## placeholders (si PlaceholderAPI est present)
Par défaut identifiant : `%ritualpulse_<placeholder>%`

Exemples:
- `%ritualpulse_streak%`
- `%ritualpulse_freeze_tokens%`
- `%ritualpulse_total_claims%`
- `%ritualpulse_cycle_day%`
- `%ritualpulse_next_claim_in%`
- `%ritualpulse_can_claim%`

## sécurité / stabilité (premium prep)
- anti double-claim local (spam clic / spam commande)
- état persisté **avant** exécution des commandes reward (réduit le risque de duplication après crash/reboot)
- warning joueur + logs console si une commande reward échoue
- fallback YAML si init SQL échoue

## docs utiles (pack vente)
- `docs/COMMANDS_PERMISSIONS.md`
- `docs/CONFIG_EXAMPLES.md`
- `docs/COMPATIBILITY_MATRIX.md`
- `docs/TEST_PLAN_PREMIUM.md`
- `docs/SPIGOTMC_PAGE_FR.md`
- `docs/CHANGELOG.md`
- `docs/ROADMAP.md`
- `docs/MEDIA_CHECKLIST.md`

## notes
- Le `reload` recharge surtout config/lang + timings. Pour changer de **type de storage** (YAML/SQL), fais un restart propre.
- MySQL/SQLite/YAML gèrent la persistance des données. Pour un réseau multi-serveur, MySQL partage les données, mais si tu veux du **zero race cross-serveur**, il faudra un lock distribué (voir roadmap).
