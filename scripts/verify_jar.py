#!/usr/bin/env python3
"""Run one already-built jar in dev and prod, then prove its bytes did not change."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request

dev_only = "--dev-only" in sys.argv
arguments = [argument for argument in sys.argv[1:] if argument != "--dev-only"]
jar = Path(arguments[0] if arguments else "target/two-environments-one-jar-1.0.0.jar").resolve()
java = os.environ.get("JAVA_HOME")
java = str(Path(java) / "bin/java") if java else "java"

def digest():
    return hashlib.sha256(jar.read_bytes()).hexdigest()

def request(port, path):
    try:
        with urllib.request.urlopen(f"http://127.0.0.1:{port}{path}", timeout=2) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as error:
        return error.code, {}

def run(profile, port):
    with tempfile.TemporaryFile(mode="w+") as log:
        process = subprocess.Popen([java, "-jar", str(jar), f"--spring.profiles.active={profile}",
                                    f"--server.port={port}"], stdout=log, stderr=subprocess.STDOUT)
        try:
            for _ in range(180):
                if process.poll() is not None:
                    raise RuntimeError(f"{profile} exited with {process.returncode}; inspect local logs without sharing credentials")
                try:
                    if request(port, "/actuator/health")[1].get("status") == "UP":
                        break
                except (OSError, ValueError):
                    pass
                time.sleep(1)
            else:
                raise RuntimeError(f"{profile} did not become healthy")
            for endpoint in ("health", "info", "metrics"):
                assert request(port, f"/actuator/{endpoint}")[0] == 200, endpoint
            for endpoint in ("env", "beans", "configprops", "loggers", "mappings", "heapdump"):
                assert request(port, f"/actuator/{endpoint}")[0] in (403, 404), endpoint
            count = len(request(port, "/api/students")[1])
            assert count == (3 if profile == "dev" else 0), "Use an empty PostgreSQL database for verification"
            print(f"PASS {profile}: database, migrations, seed isolation, Actuator allowlist")
        finally:
            process.terminate()
            try:
                process.wait(timeout=20)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()

for name in (() if dev_only else ("DB_URL", "DB_USERNAME", "DB_PASSWORD", "REGISTRAR_CONTACT_ADDRESS")):
    if not os.environ.get(name):
        raise SystemExit(f"Required environment variable: {name}")
before = digest()
run("dev", 18081)
assert digest() == before
if not dev_only:
    run("prod", 18082)
    assert digest() == before
    print(f"PASS same jar SHA-256 before/dev/prod: {before}")
else:
    print(f"PASS dev jar SHA-256 unchanged: {before}; prod not checked")
with tempfile.TemporaryFile(mode="w+") as log:
    result = subprocess.run([java, "-jar", str(jar), "--spring.profiles.active=dev",
                             "--registration.rules.waitlist-size=-1", "--server.port=0"],
                            stdout=log, stderr=subprocess.STDOUT, timeout=90)
    log.seek(0)
    failure = log.read()
    assert result.returncode != 0 and "waitlist-size" in failure, "Invalid configuration was not rejected by property name"
print("PASS invalid waitlist-size fails startup and names the property")
