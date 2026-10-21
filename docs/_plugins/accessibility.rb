# Screen-reader fixes for the just-the-docs theme under jekyll-polyglot.
#
# The theme's markup assumes a single English site. On a multilingual site that
# breaks screen readers in ways sighted visitors never notice:
#
#   * <html lang="en-US"> on every page, so a screen reader reads French, Hindi
#     or Arabic text with an English voice (and Arabic/Farsi/Urdu pages had no
#     dir="rtl" without a script).
#   * English accessible names ("Skip to main content", "Toggle menu",
#     "Breadcrumb", "toggle items in X category") that are only ever spoken,
#     never seen, so nobody notices they were never translated.
#   * A link before every heading, labelled with the heading's own text, so
#     browsing by heading or tabbing through a page announces everything twice.
#   * Nav expander buttons that use aria-pressed rather than aria-expanded, and
#     no aria-current on the current page's nav link.
#   * Search results that appear silently: nothing tells a screen reader user
#     how many results there were, or that there were none.
#   * Wide tables and code blocks that scroll sideways but can't be reached
#     with a keyboard.
#
# The site is built with whatever just-the-docs release bundler resolves
# (Gemfile.lock is not committed), so rather than copying theme templates that
# would silently drift, this rewrites the theme's rendered output. Every
# rewrite warns if its pattern stops matching, so a theme change that breaks one
# shows up in the build log instead of quietly reverting the fix.
#
# Localized strings live in _data/ui_strings.yml (the `a11y` section), keyed by
# the polyglot language code, falling back to English.
require "json"

module SoundscapeA11y
  RTL_LANGUAGES = %w[ar arz fa he iw ur ps sd ckb ug yi dv].freeze

  module_function

  def warn_once(key, message)
    @warned ||= {}
    return if @warned[key]
    @warned[key] = true
    Jekyll.logger.warn "Accessibility:", message
  end

  # Replace `pattern` in `html`, warning if it no longer appears in the theme's
  # output. `required` is false for markup that only some pages have.
  def rewrite(html, key, pattern, required: true, &block)
    found = pattern.is_a?(Regexp) ? html.match?(pattern) : html.include?(pattern)
    if required && !found
      warn_once(key, "theme markup for '#{key}' not found; that fix is no longer applied")
    end
    # The block gets the MatchData: $1 etc. would be the caller's, not gsub's.
    found ? html.gsub(pattern) { block.call(Regexp.last_match) } : html
  end

  def strings(site, lang)
    table = site.data.dig("ui_strings", "a11y") || {}
    english = table[site.config["default_lang"] || "en"] || {}
    english.merge(table[lang] || {})
  end

  def dir_for(lang)
    RTL_LANGUAGES.include?(lang.to_s.split("-").first) ? "rtl" : "ltr"
  end

  def escape_attr(text)
    text.to_s.gsub("&", "&amp;").gsub('"', "&quot;").gsub("<", "&lt;")
  end

  def escape_text(text)
    text.to_s.gsub("&", "&amp;").gsub("<", "&lt;")
  end

  def fix_page(page, site)
    html = page.output
    # Only pages rendered through the theme's default layout.
    return unless html&.include?('class="skip-to-main"')

    default_lang = site.config["default_lang"] || "en"
    # Chrome (nav, search, labels) is in the language being rendered.
    chrome_lang = site.active_lang || default_lang
    # Content is in the page's own language. An untranslated page shown under
    # /<lang>/ falls back to the English source, which has no `lang`. Pages
    # outside polyglot's localization (the translation questionnaires) say what
    # language they are in with `content_lang`.
    content_lang = page.data["content_lang"] || page.data["lang"] || default_lang
    t = strings(site, chrome_lang)

    chrome_dir = dir_for(chrome_lang)
    dir_attr = chrome_dir == "rtl" ? ' dir="rtl"' : ""
    html = rewrite(html, "html-lang", /<html lang="[^"]*">/) do
      %(<html lang="#{chrome_lang}"#{dir_attr}>)
    end

    if content_lang != chrome_lang
      content_dir = dir_for(content_lang)
      main_dir = content_dir == chrome_dir ? "" : %( dir="#{content_dir}")
      html = rewrite(html, "main-lang", /<main>/) do
        %(<main lang="#{content_lang}"#{main_dir}>)
      end
    end

    html = rewrite(html, "skip-link", %r{(<a class="skip-to-main" href="#main-content">)Skip to main content(</a>)}) do |m|
      "#{m[1]}#{escape_text(t['skip'])}#{m[2]}"
    end

    html = rewrite(html, "menu-button", 'aria-label="Toggle menu"') do
      %(aria-label="#{escape_attr(t['menu'])}")
    end

    html = rewrite(html, "site-nav-label", '<nav aria-label="Main" id="site-nav"') do
      %(<nav aria-label="#{escape_attr(t['menu'])}" id="site-nav")
    end

    html = rewrite(html, "breadcrumb-label", '<nav aria-label="Breadcrumb"', required: false) do
      %(<nav aria-label="#{escape_attr(t['breadcrumb'])}")
    end

    # Expander buttons: name them after their section and expose the state as
    # expanded/collapsed. The site's JS keeps aria-expanded in step (see
    # fix_theme_js); the current page's ancestors are expanded on load.
    html = rewrite(html, "nav-expander",
                   /aria-label="toggle items in (.*?) category" aria-pressed="false"/) do |m|
      %(aria-label="#{m[1]}" aria-expanded="false")
    end

    # Heading anchor links: keep them for mouse users, hide them from screen
    # readers and the tab order (the heading itself carries the text).
    html = rewrite(html, "heading-anchor",
                   /class="anchor-heading" aria-labelledby="[^"]*"/, required: false) do
      'class="anchor-heading" aria-hidden="true" tabindex="-1"'
    end

    # Polite live region announcing the search result count.
    html = rewrite(html, "search-status",
                   '<div id="search-results" class="search-results"></div>', required: false) do
      '<div id="search-results" class="search-results"></div>' \
        '<div id="search-status" class="visually-hidden" role="status" aria-live="polite"></div>'
    end

    i18n = { noResults: t["no_results"], results: t["results"] }
    html = html.sub("</head>", "<script>window.SOUNDSCAPE_I18N=#{JSON.generate(i18n)};</script></head>")

    page.output = html
  end

  # The theme script is a single shared file for every language, so localized
  # text comes from window.SOUNDSCAPE_I18N, which fix_page writes into each page.
  def fix_theme_js(page)
    js = page.output

    js = rewrite(js, "js-expander", "target.ariaPressed = target.parentNode.classList.toggle('active');") do
      "target.setAttribute('aria-expanded', target.parentNode.classList.toggle('active'));"
    end

    js = rewrite(js, "js-activate-nav", /(\n\s*)activateNav\(\);/) do |m|
      "#{m[1]}activateNav();#{m[1]}soundscapeSyncNav();"
    end

    js = rewrite(js, "js-no-results", "noResultsDiv.innerText = 'No results found';") do
      "noResultsDiv.innerText = soundscapeI18n('noResults', 'No results found');"
    end

    # Announce the result count once typing pauses, rather than per keystroke.
    js = rewrite(js, "js-results-count", /(\n\s*)if \(results\.length == 0\) \{(\s*)var noResultsDiv/) do |m|
      "#{m[1]}soundscapeAnnounceResults(results.length);#{m[1]}if (results.length == 0) {#{m[2]}var noResultsDiv"
    end

    js += <<~JS

      // --- Added by docs/_plugins/accessibility.rb -------------------------------
      function soundscapeI18n(key, fallback) {
        var t = window.SOUNDSCAPE_I18N || {};
        return t[key] != null ? t[key] : fallback;
      }

      var soundscapeAnnounceTimer = null;
      function soundscapeAnnounceResults(count) {
        var status = document.getElementById('search-status');
        if (!status) return;
        clearTimeout(soundscapeAnnounceTimer);
        soundscapeAnnounceTimer = setTimeout(function() {
          status.textContent = count == 0
            ? soundscapeI18n('noResults', 'No results found')
            : String(soundscapeI18n('results', 'Search results: %d')).replace('%d', count);
        }, 700);
      }

      // A table or code block wider than the page scrolls sideways; keyboard
      // users can only scroll it if it can take focus. Only boxes that really
      // scroll get a tab stop.
      function soundscapeFocusableScrollers() {
        document.querySelectorAll('.table-wrapper, .highlight').forEach(function(box) {
          var overflowX = getComputedStyle(box).overflowX;
          var scrolls = (overflowX == 'auto' || overflowX == 'scroll') && box.scrollWidth > box.clientWidth;
          if (scrolls) {
            box.setAttribute('tabindex', '0');
          } else {
            box.removeAttribute('tabindex');
          }
        });
      }
      jtd.onReady(soundscapeFocusableScrollers);
      window.addEventListener('resize', soundscapeFocusableScrollers);

      function soundscapeSyncNav() {
        var nav = document.getElementById('site-nav');
        if (!nav) return;
        var current = nav.querySelector('a.nav-list-link.active');
        if (current) current.setAttribute('aria-current', 'page');
        nav.querySelectorAll('.nav-list-item > .nav-list-expander').forEach(function(button) {
          button.setAttribute('aria-expanded', button.parentNode.classList.contains('active'));
        });
      }
    JS

    page.output = js
  end
end

Jekyll::Hooks.register [:pages, :documents], :post_render do |page|
  if page.url.to_s.end_with?("/assets/js/just-the-docs.js")
    SoundscapeA11y.fix_theme_js(page)
  elsif page.output_ext == ".html"
    SoundscapeA11y.fix_page(page, page.site)
  end
end
