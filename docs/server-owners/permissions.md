# 🔐 Permissions

All SwagSlayer permission nodes are listed below.

## Permission Reference

| Permission | Default | Description |
|-----------|---------|-------------|
| `swagslayer.use` | `true` | Open the `/slayer` GUI and view own stats |
| `swagslayer.admin` | `op` | Full admin access: `/slayadmin` all sub-commands + view other players' stats via `/slayer <player>` |

## Default Values

* **`true`** — All players have this permission by default. Revoke it from groups that should not have access.
* **`op`** — Only server operators have this permission by default.

## Notes

* Players without `swagslayer.use` will see "You don't have permission to use this command." when running `/slayer`
* The `/slayer <player>` lookup (viewing another player's stats in chat) requires `swagslayer.admin`
* All `/slayadmin` sub-commands (`reload`, `setlevel`, `addxp`, `reset`) require `swagslayer.admin`
* Console always has permission to run any `/slayadmin` sub-command

## Configuration via Permission Plugin

Use a permission plugin like LuckPerms to assign or revoke permissions:

```bash
# Revoke slayer access from a group
/lp group default permission set swagslayer.use false

# Grant admin access to a staff group
/lp group staff permission set swagslayer.admin true
```

## Related Pages

* [Admin Commands](admin-commands.md) — full admin command reference
