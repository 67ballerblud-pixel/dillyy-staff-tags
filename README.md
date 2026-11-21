# DillyyStaffTags 1.0

Red star = Dev, green star = Staff, Media badge shown above heads too. Dev > Staff > Media > everyone in the tablist.

## Install
1. Push this folder to GitHub (Actions > Build > Artifacts) or `mvn package`. Put the jar in `plugins/`, restart.
2. Give ranks by permission (use your own group names):
   /lp group <devgroup> permission set dillyytags.dev true
   /lp group <staffgroup> permission set dillyytags.staff true
   Media already uses `rank.media`.
3. For the tablist star, set the LuckPerms prefix on the same groups and add the prefix to UltimateDonutSMP's tablist:
   /lp group <devgroup> meta setprefix 100 "&c★ "
   /lp group <staffgroup> meta setprefix 90 "&a★ "
   config.yml  TABLIST.NAME-FORMAT: '%luckperms_prefix%<media_badge>&f<nick>%team_suffix%'
   then restart (or /ultimatedonutsmp reload if you have it).
4. /stafftags reload after editing plugins/DillyyStaffTags/config.yml. If staff end up at the bottom of the tablist, set reverse-tab-order: true.
