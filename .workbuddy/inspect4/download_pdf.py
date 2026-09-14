import urllib.request
import os

url = "https://ai.d.gtimg.com/mcp/exports/724676748061593/Hidden%20Camera%20Detector%20%E2%80%94%20UI%20v2%20(Apple%20System)-20260914_093724407.pdf?sign=7b30e61ddd12fdaccd4870a47b9ea431&t=1791941844"
out = r"C:\Android\driverSkr\CameraDetection\.workbuddy\exports\Hidden-Camera-Detector-UI-v2-Apple-System-FULL.pdf"
log = r"C:\Android\driverSkr\CameraDetection\.workbuddy\inspect4\download.log"

os.makedirs(os.path.dirname(out), exist_ok=True)
os.makedirs(os.path.dirname(log), exist_ok=True)

lines = ["Output dir ok: " + str(os.path.isdir(os.path.dirname(out)))]
try:
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        data = resp.read()
        lines.append("HTTP: " + str(resp.status) + " len: " + str(len(data)))
    with open(out, "wb") as f:
        f.write(data)
    lines.append("Saved: " + str(os.path.getsize(out)) + " bytes -> " + out)
except Exception as e:
    lines.append("ERROR: " + type(e).__name__ + " " + str(e))

with open(log, "w") as f:
    f.write("\n".join(lines))
print("WROTE_LOG")