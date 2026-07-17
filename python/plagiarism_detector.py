import os
import sys
import json
import requests
import zipfile
import re


# ============================================================
# CONFIGURATION
# ============================================================
# ✅ Le script ne scanne plus tout le dossier uploads : il reçoit désormais
#    en arguments le chemin du ZIP à analyser et le chemin du résultat à produire.
#    Usage : python plagiarism_detector.py <chemin_zip> <chemin_resultat.json>
#
# ✅ Le token GitHub et la clé SerpAPI ne sont plus codés en dur : ils sont lus
#    depuis les variables d'environnement, avec repli sur un fichier local
#    "config.properties" (non versionné, voir .gitignore) placé à côté de ce script.

def load_secrets() -> dict:
    secrets = {
        "GITHUB_TOKEN": os.environ.get("GITHUB_TOKEN"),
        "SERPAPI_KEY":  os.environ.get("SERPAPI_KEY"),
    }

    if not secrets["GITHUB_TOKEN"] or not secrets["SERPAPI_KEY"]:
        config_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "config.properties")
        if os.path.exists(config_path):
            with open(config_path, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if not line or line.startswith("#") or "=" not in line:
                        continue
                    key, value = line.split("=", 1)
                    key, value = key.strip(), value.strip()
                    if key == "GITHUB_TOKEN" and not secrets["GITHUB_TOKEN"]:
                        secrets["GITHUB_TOKEN"] = value
                    elif key == "SERPAPI_KEY" and not secrets["SERPAPI_KEY"]:
                        secrets["SERPAPI_KEY"] = value

    return secrets


_SECRETS      = load_secrets()
GITHUB_TOKEN  = _SECRETS["GITHUB_TOKEN"]
SERP_API_KEY  = _SECRETS["SERPAPI_KEY"]


JAVA_KEYWORDS = {
    "public", "private", "protected", "class", "interface", "extends",
    "implements", "import", "package", "static", "void", "return",
    "new", "this", "super", "if", "else", "for", "while", "try",
    "catch", "throw", "throws", "final", "abstract", "int", "boolean",
    "String", "System", "override", "@Override",
}

JAVA_KEYWORD_THRESHOLD = 4


def is_java_block(text: str) -> bool:
    words = set(text.split())
    hits  = words & JAVA_KEYWORDS
    return len(hits) >= JAVA_KEYWORD_THRESHOLD


def extract_from_pdf(pdf_bytes: bytes) -> dict:
    import fitz

    java_blocks = []
    text_blocks = []

    doc = fitz.open(stream=pdf_bytes, filetype="pdf")
    for page in doc:
        raw = page.get_text("text") or ""
        paragraphs = re.split(r"\n{2,}", raw)
        for para in paragraphs:
            para = para.strip()
            if not para:
                continue
            if is_java_block(para):
                java_blocks.append(para)
            else:
                text_blocks.append(para)
    doc.close()

    return {
        "java_code":  "\n\n".join(java_blocks),
        "plain_text": "\n\n".join(text_blocks),
    }


def load_files(zip_path: str) -> list:
    """✅ N'analyse plus tout le dossier uploads : uniquement le ZIP de la soumission sélectionnée."""
    entries = []

    if not os.path.isfile(zip_path):
        print(f"[ERREUR] Fichier ZIP introuvable : {zip_path}")
        return entries

    with zipfile.ZipFile(zip_path, "r") as z:
        for entry in z.namelist():
            if not entry.lower().endswith(".pdf"):
                continue
            with z.open(entry) as pdf_file:
                pdf_bytes = pdf_file.read()
            extracted = extract_from_pdf(pdf_bytes)
            name      = os.path.splitext(os.path.basename(entry))[0]
            entries.append({
                "name":       name,
                "java_code":  extracted["java_code"],
                "plain_text": extracted["plain_text"],
            })
            has_java = bool(extracted["java_code"].strip())
            has_text = bool(extracted["plain_text"].strip())
            print(f"[OK] {entry}  |  Java: {has_java}  Texte: {has_text}")

    print(f"\nTotal PDF traites : {len(entries)}\n")
    return entries


# 2. SIMILARITÉ (JACCARD)

def similarity(text1: str, text2: str) -> float:
    set1 = set(text1.split())
    set2 = set(text2.split())
    if not set1 or not set2:
        return 0.0
    return len(set1 & set2) / len(set1 | set2) * 100


# 3. NETTOYAGE DU CODE

def clean_code(code: str) -> str:
    code = re.sub(r"[^a-zA-Z0-9 ]", " ", code)
    code = re.sub(r"\s+", " ", code)
    return code.strip()


# 4. RECHERCHE GITHUB

def github_search(code_sample: str) -> str:
    if not GITHUB_TOKEN:
        print("  [WARN] GITHUB_TOKEN non configuré (variable d'env. ou config.properties) : recherche GitHub ignorée.")
        return "No match"
    try:
        query = clean_code(code_sample)[:80]
        url   = f"https://api.github.com/search/code?q={requests.utils.quote(query)}"
        headers = {
            "Authorization": f"token {GITHUB_TOKEN}",
            "Accept":        "application/vnd.github.v3+json",
        }
        r = requests.get(url, headers=headers, timeout=10)
        if r.status_code == 200:
            items = r.json().get("items", [])
            if items:
                return items[0]["repository"]["full_name"]
        else:
            print(f"  GitHub API error: {r.status_code}")
    except Exception as e:
        print(f"  GitHub search error: {e}")
    return "No match"


# 5. RECHERCHE WEB (TEXTE)

def web_search_serpapi(query: str) -> list[dict]:
    if not SERP_API_KEY:
        print("  [WARN] SERPAPI_KEY non configurée (variable d'env. ou config.properties) : recherche web ignorée.")
        return []
    try:
        query = query.encode('ascii', errors='ignore').decode('ascii')
        print(f"  Requete nettoyee: {repr(query)}")

        url    = "https://serpapi.com/search"
        params = {
            "q":       query,
            "api_key": SERP_API_KEY,
            "num":     3,
            "engine":  "google"
        }
        print(f"  SerpAPI requete envoyee...")
        r = requests.get(url, params=params, timeout=15)
        print(f"  SerpAPI status: {r.status_code}")
        if r.status_code == 200:
            results = r.json().get("organic_results", [])
            print(f"  SerpAPI resultats: {len(results)}")
            return [{"title": x.get("title"), "link": x.get("link")} for x in results[:3]]
        else:
            print(f"  SerpAPI error: {r.status_code} - {r.text[:200]}")
    except Exception as e:
        print(f"  SerpAPI exception: {type(e).__name__}: {e}")
    return []


def search_text_online(text: str) -> list[dict]:
    try:
        print(f"  Debut search_text_online...")
        sentences = re.split(r"[.!?]", text)
        sentences = [s.strip() for s in sentences if len(s.split()) >= 8]
        if not sentences:
            query = " ".join(text.split()[:15])
        else:
            sentences.sort(key=lambda s: abs(len(s.split()) - 15))
            query = sentences[0][:120]

        query = query.replace("\n", " ").strip()
        print(f"  Requete web : {repr(query)}")

        if SERP_API_KEY:
            return web_search_serpapi(query)
        else:
            return []
    except Exception as e:
        print(f"  search_text_online exception: {type(e).__name__}: {e}")
        return []


# 6. ANALYSE PRINCIPALE

def analyze(zip_path: str, result_file: str):
    files = load_files(zip_path)

    results = {
        "student_code_results":    [],
        "github_results":          [],
        "text_plagiarism_results": [],
    }

    # Comparaison de code Java entre étudiants
    print("=== Comparaison de code Java entre etudiants ===")
    for i in range(len(files)):
        for j in range(i + 1, len(files)):
            e1, e2 = files[i], files[j]
            if not e1["java_code"] or not e2["java_code"]:
                continue
            sim = similarity(e1["java_code"], e2["java_code"])
            results["student_code_results"].append({
                "student1":   e1["name"],
                "student2":   e2["name"],
                "similarity": round(sim, 2),
            })
            print(f"  {e1['name']} <-> {e2['name']} : {sim:.1f}%")

    # Recherche GitHub
    print("\n=== Recherche GitHub ===")
    for entry in files:
        if not entry["java_code"].strip():
            print(f"  {entry['name']} : pas de code Java extrait")
            results["github_results"].append({
                "student":    entry["name"],
                "repository": "N/A (aucun code Java extrait)",
            })
            continue
        repo = github_search(entry["java_code"])
        print(f"  {entry['name']} -> {repo}")
        results["github_results"].append({
            "student":    entry["name"],
            "repository": repo,
        })

    # Recherche de plagiat textuel
    print("\n=== Recherche de plagiat textuel ===")
    for entry in files:
        if not entry["plain_text"].strip():
            print(f"  {entry['name']} : pas de texte normal extrait")
            results["text_plagiarism_results"].append({
                "student": entry["name"],
                "sources": [],
                "note":    "Aucun texte normal extrait du PDF",
            })
            continue
        print(f"  {entry['name']} :")
        sources = search_text_online(entry["plain_text"])
        results["text_plagiarism_results"].append({
            "student": entry["name"],
            "sources": sources,
        })
        for s in sources:
            title = s['title'].encode('ascii', errors='ignore').decode('ascii')
            link  = s['link'].encode('ascii', errors='ignore').decode('ascii')
            print(f"    -> {title}  ({link})")

    # Sauvegarde : ✅ un fichier de résultat propre à cette soumission
    os.makedirs(os.path.dirname(result_file) or ".", exist_ok=True)
    with open(result_file, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=4, ensure_ascii=False)
    print(f"\nResultats sauvegardes dans {result_file}")


# =========================
# RUN
# =========================
if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python plagiarism_detector.py <chemin_zip> <chemin_resultat.json>")
        sys.exit(1)

    zip_arg    = sys.argv[1]
    result_arg = sys.argv[2]
    analyze(zip_arg, result_arg)
