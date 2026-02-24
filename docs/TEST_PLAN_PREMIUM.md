# Plan de test premium (rapide mais sérieux)

## 1) Smoke test
1. Démarrer serveur avec YAML
2. `/ritual` ouvre bien
3. `/ritual claim` donne récompense + cooldown
4. `/ritual admin inspect <toi>` affiche données cohérentes

## 2) Anti-duplication / anti double-claim
- spam clic GUI sur claim (10+ clics)
- spam `/ritual claim`
- vérifier argent/keys: **1 seul gain**

## 3) Reboot
- claim une fois
- stop serveur propre
- restart
- vérifier cooldown toujours présent
- attendre cooldown puis claim -> progression/streak ok

## 4) Erreurs config lisibles
- mettre `gui.icons.claim-ready: "ABC_NOT_A_MATERIAL"`
- `/ritual reload`
- `/ritual admin doctor`
- vérifier warning clair (sans stacktrace illisible)

## 5) Erreurs commande reward
- mettre une commande invalide dans `rewards.days.1.commands`
- claim
- vérifier:
  - claim validé (pas de dupe possible)
  - warning joueur
  - warning console avec path + index

## 6) Storages
- tester YAML puis SQLite puis MySQL
- vérifier persistence entre reboot pour chacun
