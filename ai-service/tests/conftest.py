"""Keep the ordinary pytest suite deterministic and offline.

Real-LLM acceptance is executed by the dedicated scripts under ``.agent``.
The standard test suite must never inherit developer-machine credentials during
test collection, because application settings are initialized at import time.
"""

import os


os.environ.pop("LLM_API_KEY", None)
os.environ.pop("API_KEY", None)
