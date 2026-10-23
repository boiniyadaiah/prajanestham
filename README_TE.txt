ప్రజానేస్తం షార్ట్ న్యూస్ — Android Studio Kotlin starter project
===============================================================

ముఖ్యం:
ఇది ప్రారంభ ప్రాజెక్ట్ / starter scaffold. Google Playకి నేరుగా అప్లోడ్ చేయడానికి సిద్ధమైన ఫైనల్ యాప్ కాదు.
ఈ పరిసరంలో Gradle build లేదా Android emulator పరీక్ష చేయలేదు. మీ WordPress APIలో category ID, image parsing, district taxonomy mapping ధృవీకరించాలి.

ప్రస్తుతం చేర్చినవి:
- WordPress REST API నుంచి పబ్లిక్ పోస్టుల లోడ్
- పైకి/కిందికి swipe ద్వారా వార్త మార్చడం
- జిల్లా ఎంపిక UI (ఇప్పటికి UI మాత్రమే; నిజమైన district filtering కోసం WordPress taxonomy/meta mapping కావాలి)
- Share
- local bookmarks
- dark mode preference
- Android TextToSpeech (ఫోన్‌లో Telugu TTS engine ఉంటే)
- AdMob banner కోసం Google test IDs
- Firebase Messaging service scaffold (Firebase project config ఇంకా అవసరం)

Android Studioలో ప్రారంభం:
1. ZIP extract చేయండి.
2. Android Studio > Open > extracted PrajanesthamShortNews folder.
3. Gradle sync పూర్తయ్యే వరకు వేచి ఉండండి.
4. Android SDK Platform 36 మరియు JDK 17 ఇన్‌స్టాల్ అయి ఉండాలి.
5. Run చేయడానికి USB debugging ఉన్న Android ఫోన్ లేదా emulator ఎంచుకోండి.

అత్యవసర WordPress సెటప్:
1. WordPress Dashboard > Posts > Categoriesలో “షార్ట్ న్యూస్” category ఉందో చూడండి.
2. Browserలో https://www.prajanestham.com/wp-json/wp/v2/categories?search=షార్ట్%20న్యూస్ తెరవండి.
3. ఫలితంలోని id సంఖ్యను app/src/main/java/com/prajanestham/shortnews/NewsApi.kt లో SHORT_NEWS_CATEGORY_ID వద్ద పెట్టండి.
4. APIలో featured image కోసం _embed response వస్తుందో తనిఖీ చేయండి. ప్రస్తుత starterలో featured image parsing TODOగా ఉంది.
5. జిల్లా ఫిల్టర్ పనిచేయాలంటే district taxonomy లేదా post meta ఎలా సేవ్ అవుతుందో నిర్ణయించి API queryకి అనుసంధానించాలి.
6. ప్రస్తుతం API public published posts మాత్రమే చదవాలి; status=draft/pending కోసం యాప్‌కు credentials ఇవ్వకండి.

Firebase Push:
1. Firebase Consoleలో Android app package com.prajanestham.shortnews నమోదు చేయండి.
2. google-services.json ఫైల్‌ను app/ లో పెట్టండి.
3. app/build.gradle.ktsలో Google Services plugin comment తీసేయండి; project plugin ఇప్పటికే root build fileలో ఉంది.
4. Firebase Cloud Messagingలో notification పంపి పరీక్షించండి.
5. WordPress నుంచి కొత్త short news publish అయినప్పుడు push పంపాలంటే WordPress plugin లేదా సురక్షిత server-side Firebase HTTP v1 integration అవసరం. Server keyని Androidలో ఎప్పుడూ ఉంచకండి.

AdMob:
- ప్రస్తుతం manifest App ID, banner unit రెండూ Google test IDs మాత్రమే.
- AdMobలో మీ app నమోదు చేసి నిజమైన App ID, Ad Unit ID పొందిన తర్వాత మాత్రమే వాటిని మార్చండి.
- మీ స్వంత live adsపై క్లిక్ చేయకండి; testing సమయంలో test ads మాత్రమే.
- పూర్తి స్క్రీన్/ఇంటర్‌స్టీషియల్ ప్రకటనలు ఇంకా అమలు చేయలేదు.

Google Play:
1. Play Console developer account సృష్టించి verification పూర్తి చేయండి.
2. Privacy Policy URL ప్రచురించండి; data safety form, ads declaration, content rating, app access వివరాలు నింపండి.
3. Play Consoleలో ఆ సమయంలో అవసరమైన target API, signed Android App Bundle (.aab), store listing assets సిద్ధం చేయండి.
4. కొత్త వ్యక్తిగత developer accountsకు వర్తించే closed testing అవసరాలను Play Consoleలో తనిఖీ చేయండి.
5. Reviewకి పంపే ముందు నిజమైన ఫోన్‌లలో పరీక్షించండి. News appలో అసలు సంపాదకీయ విలువ ఉండాలి; కేవలం వెబ్‌సైట్ wrapperలా ఉండకూడదు.

తదుపరి పనులు:
- నిజమైన category ID + district taxonomy/meta structure ఇవ్వండి.
- _embed featured image parsing పూర్తి చేయాలి.
- swipe gesturesను vertical pager/card feedగా మెరుగుపరచాలి.
- bookmarks కోసం వేరు జాబితా స్క్రీన్, district filtering, error/loading states పూర్తి చేయాలి.
- push publish automation, privacy policy, consent/ad configuration పూర్తి చేయాలి.
