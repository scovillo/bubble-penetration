const baseSections = [
  [
    'What we collect',
    '<p>Bubble Penetration does not require registration, an account, advertising or purchases. The app stores your chosen fantasy username and local high scores on your device.</p>',
  ],
  [
    'Online leaderboard',
    '<p>When the online leaderboard is enabled, the app sends your fantasy username and score to the configured backend. The score is associated with that nickname, not with an account or real name. You can use your own compatible leaderboard or disable online play and remain offline.</p>',
  ],
  [
    'Server logs and security',
    '<p>The backend records limited technical request information such as request time, URL, status and user-agent for operation, abuse prevention and rate limiting. We do not use this information for advertising or profiling.</p>',
  ],
  [
    'Retention and deletion',
    '<p>Local data is deleted when you clear the app data or uninstall the app. Online scores remain in the configured leaderboard until they are removed by its operator. To request removal from the official leaderboard, contact the maintainer through the Codeberg repository.</p>',
  ],
  [
    'Children and privacy',
    '<p>The game is designed to minimize data collection and can be played offline. Please do not enter real names, contact details or other personal information as a username.</p>',
  ],
  [
    'Third-party services',
    '<p>The landing page links to Codeberg Releases and F-Droid. Those services process data under their own privacy policies when you visit them. The app itself contains no analytics SDK, advertising SDK or payment service.</p>',
  ],
  [
    'Your rights and contact',
    '<p>Depending on your location, you may have rights to access, correct or erase personal data and to object to processing. The service operator is Lukas Scheerer. Contact and removal requests can be made via <a href="https://codeberg.org/scovillo/bubble-penetration">the project repository</a>.</p>',
  ],
];
const deSections = [
  [
    'Welche Daten wir erheben',
    '<p>Bubble Penetration benötigt weder Registrierung noch Benutzerkonto und enthält keine Werbung oder Käufe. Der von dir gewählte Fantasie-Benutzername und lokale Bestleistungen werden auf deinem Gerät gespeichert.</p>',
  ],
  [
    'Online-Bestenliste',
    '<p>Wenn die Online-Bestenliste aktiviert ist, übermittelt die App deinen Fantasie-Benutzernamen und deine Punktzahl an das konfigurierte Backend. Die Punktzahl ist mit diesem Spitznamen verknüpft, nicht mit einem Konto oder deinem echten Namen. Du kannst eine eigene kompatible Bestenliste verwenden oder vollständig offline spielen.</p>',
  ],
  [
    'Server-Logs und Sicherheit',
    '<p>Das Backend protokolliert begrenzte technische Informationen wie Zeitpunkt, URL, Status und User-Agent, um den Dienst zu betreiben, Missbrauch zu verhindern und Anfragen zu begrenzen. Diese Informationen werden nicht für Werbung oder Profile verwendet.</p>',
  ],
  [
    'Speicherung und Löschung',
    '<p>Lokale Daten werden gelöscht, wenn du die App-Daten löschst oder die App deinstallierst. Online-Punkte bleiben in der jeweiligen Bestenliste, bis sie durch deren Betreiber entfernt werden. Für eine Löschung in der offiziellen Bestenliste kontaktiere bitte den Maintainer über das Codeberg-Repository.</p>',
  ],
  [
    'Kinder und Datenschutz',
    '<p>Das Spiel minimiert die Datenerhebung und kann offline gespielt werden. Bitte verwende keine echten Namen, Kontaktdaten oder sonstigen persönlichen Informationen als Benutzernamen.</p>',
  ],
  [
    'Drittanbieter',
    '<p>Die Landingpage verlinkt auf Codeberg Releases und F-Droid. Beim Besuch dieser Angebote gelten deren eigene Datenschutzbestimmungen. Die App enthält kein Analyse-, Werbe- oder Bezahlsystem.</p>',
  ],
  [
    'Rechte und Kontakt',
    '<p>Je nach Aufenthaltsort hast du möglicherweise Rechte auf Auskunft, Berichtigung, Löschung und Widerspruch. Verantwortlich ist Lukas Scheerer. Kontakt- und Löschanfragen kannst du über <a href="https://codeberg.org/scovillo/bubble-penetration">das Projekt-Repository</a> stellen.</p>',
  ],
];
const esSections = [
  [
    'Qué recopilamos',
    '<p>Bubble Penetration no requiere registro ni cuenta y no incluye publicidad ni compras. La aplicación guarda en tu dispositivo el nombre de fantasía que elijas y tus puntuaciones locales.</p>',
  ],
  [
    'Clasificación en línea',
    '<p>Cuando la clasificación en línea está activada, la aplicación envía tu nombre de fantasía y puntuación al servidor configurado. La puntuación queda asociada a ese apodo, no a una cuenta ni a tu nombre real. Puedes usar una clasificación compatible propia o desactivar el juego en línea y permanecer sin conexión.</p>',
  ],
  [
    'Registros del servidor y seguridad',
    '<p>El servidor registra información técnica limitada, como la hora de la solicitud, la URL, el estado y el agente de usuario, para operar el servicio, prevenir abusos y limitar solicitudes. No utilizamos esta información para publicidad ni elaboración de perfiles.</p>',
  ],
  [
    'Conservación y eliminación',
    '<p>Los datos locales se eliminan al borrar los datos de la aplicación o desinstalarla. Las puntuaciones en línea permanecen en la clasificación configurada hasta que su operador las elimine. Para solicitar la eliminación de la clasificación oficial, contacta con el responsable mediante el repositorio de Codeberg.</p>',
  ],
  [
    'Menores y privacidad',
    '<p>El juego está diseñado para minimizar la recopilación de datos y puede jugarse sin conexión. No uses nombres reales, datos de contacto ni otra información personal como nombre de usuario.</p>',
  ],
  [
    'Servicios de terceros',
    '<p>La página principal enlaza a Codeberg Releases y F-Droid. Al visitarlos, esos servicios tratan los datos conforme a sus propias políticas de privacidad. La aplicación no contiene sistemas de análisis, publicidad ni pagos.</p>',
  ],
  [
    'Tus derechos y contacto',
    '<p>Según tu ubicación, puedes tener derecho a acceder, rectificar o eliminar datos personales y a oponerte a su tratamiento. El operador del servicio es Lukas Scheerer. Puedes enviar solicitudes de contacto y eliminación mediante <a href="https://codeberg.org/scovillo/bubble-penetration">el repositorio del proyecto</a>.</p>',
  ],
];
const frSections = [
  [
    'Données collectées',
    '<p>Bubble Penetration ne nécessite ni inscription ni compte et ne contient ni publicité ni achats. L’application conserve sur votre appareil le pseudonyme de votre choix et vos meilleurs scores locaux.</p>',
  ],
  [
    'Classement en ligne',
    '<p>Lorsque le classement en ligne est activé, l’application envoie votre pseudonyme et votre score au serveur configuré. Le score est associé à ce pseudonyme, et non à un compte ou à votre nom réel. Vous pouvez utiliser votre propre classement compatible ou désactiver le jeu en ligne et rester hors connexion.</p>',
  ],
  [
    'Journaux du serveur et sécurité',
    '<p>Le serveur enregistre des informations techniques limitées, telles que l’heure de la requête, l’URL, le statut et l’agent utilisateur, afin d’assurer le fonctionnement du service, de prévenir les abus et de limiter les requêtes. Ces informations ne sont utilisées ni à des fins publicitaires ni pour établir des profils.</p>',
  ],
  [
    'Conservation et suppression',
    '<p>Les données locales sont supprimées lorsque vous effacez les données de l’application ou la désinstallez. Les scores en ligne restent dans le classement configuré jusqu’à leur suppression par son opérateur. Pour demander une suppression du classement officiel, contactez le responsable via le dépôt Codeberg.</p>',
  ],
  [
    'Enfants et vie privée',
    '<p>Le jeu est conçu pour réduire au minimum la collecte de données et peut être utilisé hors connexion. N’utilisez pas de nom réel, de coordonnées ou d’autres informations personnelles comme pseudonyme.</p>',
  ],
  [
    'Services tiers',
    '<p>La page d’accueil contient des liens vers Codeberg Releases et F-Droid. Lorsque vous les consultez, ces services traitent les données conformément à leurs propres politiques de confidentialité. L’application ne contient aucun système d’analyse, de publicité ou de paiement.</p>',
  ],
  [
    'Vos droits et contact',
    '<p>Selon votre lieu de résidence, vous pouvez disposer de droits d’accès, de rectification ou d’effacement de vos données personnelles, ainsi que d’un droit d’opposition au traitement. L’opérateur du service est Lukas Scheerer. Les demandes de contact et de suppression peuvent être adressées via <a href="https://codeberg.org/scovillo/bubble-penetration">le dépôt du projet</a>.</p>',
  ],
];
const jaSections = [
  [
    '収集するデータ',
    '<p>Bubble Penetration は登録やアカウントを必要とせず、広告や購入機能もありません。選択したニックネームと端末内のハイスコアは、お使いの端末に保存されます。</p>',
  ],
  [
    'オンラインランキング',
    '<p>オンラインランキングを有効にすると、アプリはニックネームとスコアを設定済みのサーバーへ送信します。スコアはアカウントや実名ではなく、そのニックネームに関連付けられます。互換性のある独自のランキングを利用するか、オンラインプレイを無効にしてオフラインで遊ぶこともできます。</p>',
  ],
  [
    'サーバーログとセキュリティ',
    '<p>サーバーは、サービス運営、不正利用の防止、リクエスト制限のため、リクエスト日時、URL、ステータス、ユーザーエージェントなどの限定的な技術情報を記録します。これらの情報を広告やプロファイリングには使用しません。</p>',
  ],
  [
    '保存期間と削除',
    '<p>端末内のデータは、アプリのデータを消去するかアプリをアンインストールすると削除されます。オンラインスコアは、運営者が削除するまで設定済みのランキングに残ります。公式ランキングからの削除を希望する場合は、Codeberg リポジトリから管理者へご連絡ください。</p>',
  ],
  [
    '子どものプライバシー',
    '<p>本ゲームはデータ収集を最小限に抑えるよう設計され、オフラインでも遊べます。ユーザー名には、実名、連絡先、その他の個人情報を入力しないでください。</p>',
  ],
  [
    '第三者サービス',
    '<p>ランディングページには Codeberg Releases と F-Droid へのリンクがあります。これらを訪問した場合、各サービスはそれぞれのプライバシーポリシーに基づいてデータを処理します。アプリ自体には、解析、広告、決済の各システムは含まれていません。</p>',
  ],
  [
    '利用者の権利と連絡先',
    '<p>お住まいの地域によっては、個人データへのアクセス、訂正、削除、処理への異議申立てを行う権利があります。サービス運営者は Lukas Scheerer です。お問い合わせや削除の依頼は、<a href="https://codeberg.org/scovillo/bubble-penetration">プロジェクトのリポジトリ</a>から行えます。</p>',
  ],
];
const ptSections = [
  [
    'O que coletamos',
    '<p>Bubble Penetration não exige cadastro nem conta e não contém publicidade ou compras. O aplicativo armazena no seu dispositivo o nome de fantasia escolhido e as pontuações locais.</p>',
  ],
  [
    'Ranking online',
    '<p>Quando o ranking online está ativado, o aplicativo envia seu nome de fantasia e sua pontuação ao servidor configurado. A pontuação fica associada a esse apelido, não a uma conta ou ao seu nome real. Você pode usar um ranking compatível próprio ou desativar o jogo online e permanecer offline.</p>',
  ],
  [
    'Registros do servidor e segurança',
    '<p>O servidor registra informações técnicas limitadas, como horário da solicitação, URL, status e agente do usuário, para operar o serviço, prevenir abusos e limitar solicitações. Não usamos essas informações para publicidade ou criação de perfis.</p>',
  ],
  [
    'Retenção e exclusão',
    '<p>Os dados locais são excluídos quando você limpa os dados do aplicativo ou o desinstala. As pontuações online permanecem no ranking configurado até serem removidas pelo operador. Para solicitar a remoção do ranking oficial, entre em contato com o responsável pelo repositório do Codeberg.</p>',
  ],
  [
    'Crianças e privacidade',
    '<p>O jogo foi desenvolvido para minimizar a coleta de dados e pode ser jogado offline. Não use nomes reais, dados de contato ou outras informações pessoais como nome de usuário.</p>',
  ],
  [
    'Serviços de terceiros',
    '<p>A página inicial contém links para Codeberg Releases e F-Droid. Ao visitá-los, esses serviços tratam dados conforme suas próprias políticas de privacidade. O aplicativo não contém sistemas de análise, publicidade ou pagamento.</p>',
  ],
  [
    'Seus direitos e contato',
    '<p>Dependendo da sua localização, você pode ter direitos de acesso, correção ou exclusão de dados pessoais e de oposição ao tratamento. O operador do serviço é Lukas Scheerer. Solicitações de contato e exclusão podem ser feitas pelo <a href="https://codeberg.org/scovillo/bubble-penetration">repositório do projeto</a>.</p>',
  ],
];
const ruSections = [
  [
    'Какие данные мы собираем',
    '<p>Bubble Penetration не требует регистрации или учётной записи и не содержит рекламы или покупок. Выбранный вами вымышленный никнейм и локальные рекорды хранятся на вашем устройстве.</p>',
  ],
  [
    'Онлайн-рейтинг',
    '<p>Когда онлайн-рейтинг включён, приложение отправляет ваш вымышленный никнейм и результат на настроенный сервер. Результат связан с этим никнеймом, а не с учётной записью или настоящим именем. Вы можете использовать собственный совместимый рейтинг или отключить сетевую игру и оставаться офлайн.</p>',
  ],
  [
    'Журналы сервера и безопасность',
    '<p>Для работы сервиса, предотвращения злоупотреблений и ограничения запросов сервер записывает минимальные технические сведения, например время запроса, URL, статус и user-agent. Мы не используем эту информацию для рекламы или профилирования.</p>',
  ],
  [
    'Хранение и удаление',
    '<p>Локальные данные удаляются при очистке данных приложения или его удалении. Онлайн-результаты остаются в настроенном рейтинге, пока оператор не удалит их. Чтобы запросить удаление из официального рейтинга, свяжитесь с сопровождающим через репозиторий Codeberg.</p>',
  ],
  [
    'Дети и конфиденциальность',
    '<p>Игра спроектирована так, чтобы свести сбор данных к минимуму, и может работать офлайн. Не используйте в качестве имени пользователя настоящие имена, контактные данные или другую личную информацию.</p>',
  ],
  [
    'Сторонние сервисы',
    '<p>На главной странице есть ссылки на Codeberg Releases и F-Droid. При переходе на них эти сервисы обрабатывают данные в соответствии со своими политиками конфиденциальности. Само приложение не содержит систем аналитики, рекламы или оплаты.</p>',
  ],
  [
    'Ваши права и контакты',
    '<p>В зависимости от вашего местонахождения вы можете иметь право на доступ, исправление или удаление персональных данных, а также право возражать против их обработки. Оператор сервиса — Lukas Scheerer. Связаться с нами или запросить удаление можно через <a href="https://codeberg.org/scovillo/bubble-penetration">репозиторий проекта</a>.</p>',
  ],
];
const ukSections = [
  [
    'Які дані ми збираємо',
    '<p>Bubble Penetration не потребує реєстрації чи облікового запису й не містить реклами або покупок. Обране вами вигадане ім’я користувача та локальні рекорди зберігаються на вашому пристрої.</p>',
  ],
  [
    'Онлайн-рейтинг',
    '<p>Коли онлайн-рейтинг увімкнено, застосунок надсилає ваше вигадане ім’я користувача та результат на налаштований сервер. Результат пов’язаний із цим псевдонімом, а не з обліковим записом чи справжнім ім’ям. Ви можете використовувати власний сумісний рейтинг або вимкнути мережеву гру й залишатися офлайн.</p>',
  ],
  [
    'Журнали сервера та безпека',
    '<p>Для роботи сервісу, запобігання зловживанням і обмеження запитів сервер записує мінімальні технічні відомості, як-от час запиту, URL, статус і user-agent. Ми не використовуємо цю інформацію для реклами чи профілювання.</p>',
  ],
  [
    'Зберігання та видалення',
    '<p>Локальні дані видаляються, коли ви очищуєте дані застосунку або видаляєте його. Онлайн-результати залишаються в налаштованому рейтингу, доки оператор їх не видалить. Щоб подати запит на видалення з офіційного рейтингу, зверніться до супровідника через репозиторій Codeberg.</p>',
  ],
  [
    'Діти та приватність',
    '<p>Гру створено так, щоб звести збір даних до мінімуму, і в неї можна грати офлайн. Не використовуйте як ім’я користувача справжні імена, контактні дані чи іншу особисту інформацію.</p>',
  ],
  [
    'Сторонні сервіси',
    '<p>На головній сторінці є посилання на Codeberg Releases і F-Droid. Під час переходу ці сервіси обробляють дані відповідно до власних політик конфіденційності. Сам застосунок не містить систем аналітики, реклами чи оплати.</p>',
  ],
  [
    'Ваші права та контакти',
    '<p>Залежно від вашого місцезнаходження ви можете мати право на доступ, виправлення чи видалення персональних даних, а також право заперечувати проти їх обробки. Оператор сервісу — Lukas Scheerer. Зв’язатися з нами або подати запит на видалення можна через <a href="https://codeberg.org/scovillo/bubble-penetration">репозиторій проєкту</a>.</p>',
  ],
];
const zhSections = [
  [
    '我们收集的信息',
    '<p>Bubble Penetration 无需注册或创建账户，也不含广告或购买功能。应用会在你的设备上保存所选的虚构用户名和本地最高分。</p>',
  ],
  [
    '在线排行榜',
    '<p>启用在线排行榜后，应用会将你的虚构用户名和分数发送到配置的服务器。分数只与该昵称关联，不与账户或真实姓名关联。你可以使用自己的兼容排行榜，也可以关闭在线游戏并保持离线。</p>',
  ],
  [
    '服务器日志与安全',
    '<p>为保障服务运行、防止滥用和限制请求，服务器会记录少量技术信息，例如请求时间、URL、状态和用户代理。我们不会将这些信息用于广告或用户画像。</p>',
  ],
  [
    '保留与删除',
    '<p>清除应用数据或卸载应用时，本地数据会被删除。在线分数会保留在配置的排行榜中，直至其运营者将其删除。如需从官方排行榜中删除，请通过 Codeberg 仓库联系维护者。</p>',
  ],
  [
    '儿童与隐私',
    '<p>本游戏以尽量减少数据收集为设计原则，并可离线游玩。请勿使用真实姓名、联系方式或其他个人信息作为用户名。</p>',
  ],
  [
    '第三方服务',
    '<p>主页包含指向 Codeberg Releases 和 F-Droid 的链接。访问这些服务时，它们会按照各自的隐私政策处理数据。应用本身不包含分析、广告或支付系统。</p>',
  ],
  [
    '你的权利与联系方式',
    '<p>根据你所在的地区，你可能有权访问、更正或删除个人数据，并有权反对相关处理。服务运营者为 Lukas Scheerer。你可以通过<a href="https://codeberg.org/scovillo/bubble-penetration">项目仓库</a>联系我们或提交删除请求。</p>',
  ],
];
export const privacy = {
  en: {
    title: 'Privacy policy',
    description:
      'How Bubble Penetration handles local usernames, optional online highscores and technical service data.',
    eyebrow: 'Clear by design',
    intro:
      'Bubble Penetration is built for quick fun with data minimization at its core.',
    back: 'Back to website',
    updated: 'Last updated: 29 August 2026',
    sections: baseSections,
  },
  de: {
    title: 'Datenschutzerklärung',
    description:
      'Wie Bubble Penetration lokale Benutzernamen, optionale Online-Highscores und technische Servicedaten verarbeitet.',
    eyebrow: 'Klar und bewusst',
    intro:
      'Bubble Penetration ist für schnellen Spielspaß gemacht – mit Datensparsamkeit als Grundprinzip.',
    back: 'Zur Webseite',
    updated: 'Zuletzt aktualisiert: 29. August 2026',
    sections: deSections,
  },
  es: {
    title: 'Política de privacidad',
    description:
      'Cómo trata Bubble Penetration los nombres locales, las puntuaciones en línea opcionales y los datos técnicos del servicio.',
    eyebrow: 'Privacidad desde el diseño',
    intro:
      'Bubble Penetration está pensado para divertirse rápidamente y minimizar los datos desde el principio.',
    back: 'Volver al sitio web',
    updated: 'Última actualización: 29 de agosto de 2026',
    sections: esSections,
  },
  fr: {
    title: 'Politique de confidentialité',
    description:
      'Comment Bubble Penetration traite les pseudonymes locaux, les scores en ligne facultatifs et les données techniques du service.',
    eyebrow: 'Clair par conception',
    intro:
      'Bubble Penetration est conçu pour un plaisir immédiat tout en réduisant les données au minimum.',
    back: 'Retour au site',
    updated: 'Dernière mise à jour : 29 août 2026',
    sections: frSections,
  },
  ja: {
    title: 'プライバシーポリシー',
    description:
      'Bubble Penetration における端末内のユーザー名、任意のオンラインスコア、技術的なサービスデータの取り扱いについて。',
    eyebrow: 'プライバシーを第一に',
    intro:
      'Bubble Penetration は、データ収集を最小限に抑えながら手軽に楽しめるよう設計されています。',
    back: 'ウェブサイトに戻る',
    updated: '最終更新日：2026年8月29日',
    sections: jaSections,
  },
  'pt-BR': {
    title: 'Política de privacidade',
    description:
      'Como Bubble Penetration trata nomes locais, pontuações online opcionais e dados técnicos do serviço.',
    eyebrow: 'Privacidade desde o início',
    intro:
      'Bubble Penetration foi criado para diversão rápida, com a minimização de dados como princípio central.',
    back: 'Voltar ao site',
    updated: 'Última atualização: 29 de agosto de 2026',
    sections: ptSections,
  },
  ru: {
    title: 'Политика конфиденциальности',
    description:
      'Как Bubble Penetration обрабатывает локальные имена пользователей, необязательные онлайн-рекорды и технические данные сервиса.',
    eyebrow: 'Конфиденциальность по умолчанию',
    intro:
      'Bubble Penetration создана для быстрых игр с минимальным сбором данных.',
    back: 'Вернуться на сайт',
    updated: 'Последнее обновление: 29 августа 2026 г.',
    sections: ruSections,
  },
  uk: {
    title: 'Політика конфіденційності',
    description:
      'Як Bubble Penetration обробляє локальні імена користувачів, необов’язкові онлайн-рекорди й технічні дані сервісу.',
    eyebrow: 'Приватність за задумом',
    intro:
      'Bubble Penetration створено для швидких ігор із мінімальним збором даних.',
    back: 'Повернутися на сайт',
    updated: 'Останнє оновлення: 29 серпня 2026 р.',
    sections: ukSections,
  },
  'zh-CN': {
    title: '隐私政策',
    description:
      'Bubble Penetration 如何处理本地用户名、可选在线高分和技术服务数据。',
    eyebrow: '隐私融入设计',
    intro:
      'Bubble Penetration 以数据最小化为核心，为你提供轻松快捷的游戏体验。',
    back: '返回网站',
    updated: '最后更新：2026年8月29日',
    sections: zhSections,
  },
};
