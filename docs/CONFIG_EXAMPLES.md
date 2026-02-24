# Exemples de config (plugins courants)

## EssentialsX Economy
```yml
rewards:
  days:
    "1":
      commands:
        - "eco give %player% 100"
```

## CMI Economy
```yml
rewards:
  days:
    "1":
      commands:
        - "money give %player% 100"
```

## LuckPerms (bonus grade / meta)
```yml
milestones:
  "30":
    commands:
      - "lp user %player% permission settemp myserver.ritual.30d true 30d"
```

## Crates (selon plugin)
```yml
vip-bonuses:
  vip:
    permission: "group.vip"
    commands:
      - "crate key give %player% vote 1"
```

## Broadcast vanilla / Paper
```yml
milestones:
  "7":
    commands:
      - "broadcast &a%player% a une streak de &e7 jours&a !"
```

## Notes importantes
- Les commandes sont exécutées en console.
- Tu peux mettre `%player%`, `%uuid%`, `%streak%`, `%day%`, `%total_claims%`, `%freeze_tokens%`.
- Si PlaceholderAPI est présent, les placeholders PAPI peuvent aussi être remplacés.
- Si une commande renvoie `false` ou throw une erreur, le claim reste validé (sécurité anti-duplication), et le joueur reçoit un warning si activé.
