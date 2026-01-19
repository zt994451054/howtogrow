const DANGEROUS_TAGS = new Set(["script", "iframe", "object", "embed"]);
const URL_ATTRS = new Set(["href", "src", "poster"]);

function isJavaScriptUrl(value: string): boolean {
  return value.trim().toLowerCase().startsWith("javascript:");
}

export function sanitizeHtmlForDisplay(html: string): string {
  if (!html.trim()) return "";

  const parser = new DOMParser();
  const doc = parser.parseFromString(html, "text/html");

  const dangerousElements = doc.querySelectorAll(Array.from(DANGEROUS_TAGS).join(","));
  dangerousElements.forEach((el) => el.remove());

  const allElements = doc.querySelectorAll("*");
  allElements.forEach((el) => {
    for (const attr of Array.from(el.attributes)) {
      const name = attr.name.toLowerCase();
      if (name.startsWith("on")) {
        el.removeAttribute(attr.name);
        continue;
      }
      if (URL_ATTRS.has(name) && isJavaScriptUrl(attr.value)) {
        el.removeAttribute(attr.name);
      }
    }
  });

  return doc.body.innerHTML;
}

