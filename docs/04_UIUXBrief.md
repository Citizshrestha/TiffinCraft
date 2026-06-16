━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
AESTHETIC DIRECTION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Style:       Warm, minimal, premium, clean
Feel:        Trustworthy, homemade, natural
Vibe:        Like a premium food startup —
             not a cheap local directory app
Reference:   Zomato (clean cards), Swiggy
             (smooth flow), Notion (minimal)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
COLOR PALETTE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Primary Green:      #2E7D32  (buttons, CTAs)
Light Green:        #4CAF50  (active dots,
                              success states)
Background:         #FAF8F5  (warm cream —
                              all screens)
Card Background:    #FFFFFF
Title Text:         #1A1A1A
Body Text:          #666666
Muted Text:         #999999
Input Border:       #E0DEDA
Input Focused:      #2E7D32
Error Red:          #D32F2F
Success Green:      #388E3C
Link Color:         #2E7D32
Badge/Chip bg:      #E8F5E9
Divider:            #F0EDE8
Avatar bg circle:   #F5F0E8

Splash Screen Only:
  Background:       #F5F0E8  (cream)
  Title:            #3B2314  (dark brown)
  Accent bar:       #C98A0A  (saffron gold)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TYPOGRAPHY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
App Name / Hero:    serif, bold, 42–44sp
Screen Titles:      sans-serif, bold, 24–26sp
Section Titles:     sans-serif, bold, 18–20sp
Card Titles:        sans-serif, bold, 16sp
Body Text:          sans-serif, normal, 14–15sp
Captions:           sans-serif, normal, 12–13sp
Buttons:            sans-serif, bold, 15sp
                    letterSpacing: 0.08

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
COMPONENT STYLE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Border Radius:
  Buttons:    14dp (main CTA)
  Cards:      16–20dp
  Inputs:     12dp
  Avatars:    oval (full circle)
  Chips:      20dp
  Dots:       oval or pill shape

Elevation / Shadow:
  Cards:      6dp elevation
  Buttons:    4dp elevation
  Google btn: 2dp elevation
  Input:      0dp (border instead)

Inputs:
  Use TextInputLayout (Material)
  Floating label effect
  Green border on focus
  Eye toggle on password fields
  Error shown inline below field

Buttons:
  Primary: solid green #2E7D32
  Secondary: outlined green border
  Disabled: opacity 0.5
  Loading: show CircularProgressIndicator
  stateListAnimator: @null (flat look)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
SPACING SYSTEM
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Screen padding:       28dp left/right
Section gap:          24dp
Card internal pad:    16–20dp
Between elements:     8dp, 16dp, 24dp
Between cards:        12–16dp
Bottom safe area:     32dp

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
DARK / LIGHT MODE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Light mode only for MVP.
Dark mode in Version 2.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
KEY UI PATTERNS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- Cards with rounded corners and elevation
  for cook profiles and meals
- RecyclerView for all lists (cooks, meals,
  orders, reviews)
- Pill-shaped active dot indicator
  (20dp wide × 8dp tall) for onboarding
- Role badge chip on register screen
- Cook avatar in circular frame
  on cream background
- Ripple effect on all clickable cards
- Status timeline for order tracking
- Bottom navigation bar on home screens
- FloatingActionButton for add meal (cook)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
MOBILE SPECIFICS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- All screens inside ScrollView where
  content might exceed screen height
- Keyboard pushes content up automatically
- Status bar color matches screen background
- No action bar — custom top bars only
- Theme: Theme.TiffinCraft.NoActionBar
- Touch targets minimum 48dp × 48dp
- Images use Glide for async loading
  with placeholder while loading