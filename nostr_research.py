import requests

def search_maven(query):
    url = f"https://search.maven.org/solrsearch/select?q={query}&rows=20&wt=json"
    response = requests.get(url)
    if response.status_code == 200:
        return response.json()
    return None

print("Searching for Nostr libraries...")
# Searching for common Nostr library patterns
results = search_maven("nostr")
if results:
    for doc in results.get('response', {}).get('docs', []):
        print(f"{doc['g']}:{doc['a']}:{doc['v']}")

results = search_maven("vitorpamplona")
if results:
    for doc in results.get('response', {}).get('docs', []):
        print(f"{doc['g']}:{doc['a']}:{doc['v']}")
