# Make just-the-docs' Lunr search work per-language.
#
# Every page (including /<lang>/ pages) loads the SAME root
# `/assets/js/just-the-docs.js` — the <script src> isn't localized because
# `assets` is in exclude_from_localization. That shared script has two
# English-only hardcodings we fix at build time so the one script behaves
# correctly for whatever language the visitor is currently viewing:
#
#   1. The search index URL (`request.open('GET', '<base>/assets/js/search-data.json')`)
#      always pointed at the English index, so translated pages searched English.
#   2. The result link (`resultLink.setAttribute('href', doc.url)`) uses the URL
#      stored in search-data.json, which is the root/English URL (translated pages
#      deliberately share the English permalink for polyglot pairing), so clicking
#      a French result navigated to the English page.
#
# We rewrite both into tiny runtime expressions that derive the active language
# prefix from window.location.pathname (e.g. "/fr") and apply it.
require "json"

Jekyll::Hooks.register :site, :post_render do |site|
  base = site.config["baseurl"].to_s
  langs = (site.config["languages"] || []).to_json
  default = (site.config["default_lang"] || "en")
  static_url = "#{base}/assets/js/search-data.json"

  # JS IIFE returning the active language path prefix: "/fr" for a /fr/ page,
  # "" for the default language or any non-language path.
  prefix = "(function(){" \
           "var b=#{base.inspect};var L=#{langs};var d=#{default.inspect};" \
           "var p=window.location.pathname;" \
           "var r=p.indexOf(b)===0?p.slice(b.length):p;" \
           "var s=r.split('/').filter(Boolean)[0];" \
           "return (L.indexOf(s)>=0&&s!==d)?('/'+s):'';})()"

  # 1. Per-language search index URL.
  search_url = "(#{base.inspect}+#{prefix}+'/assets/js/search-data.json')"
  # 2. Prefix the result URL with the active language (only if it starts with baseurl).
  href = "(function(u){var b=#{base.inspect};var x=#{prefix};" \
         "return (u && u.indexOf(b)===0)?(b+x+u.slice(b.length)):u;})(doc.url)"

  site.pages.each do |page|
    next unless page.output
    next unless page.url.to_s.end_with?("/assets/js/just-the-docs.js")
    out = page.output
    out = out.sub("'#{static_url}', true)", "#{search_url}, true)")
    out = out.sub("setAttribute('href', doc.url)", "setAttribute('href', #{href})")
    page.output = out
  end
end
