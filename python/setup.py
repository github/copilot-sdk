#simplify setup.py to use pyproject.toml as source of truth

from setuptools import setup, find_packages

setup(
    name="github-copilot-sdk",
    version="0.1.0",
    packages=find_packages(include=["copilot*"]),
    install_requires=[
        "python-dateutil>=2.9.0.post0",
        "pydantic>=2.0",
        "typing-extensions>=4.0.0",
    ],
    extras_require={
        "dev": [
            "ruff>=0.1.0",
            "ty>=0.0.2",
            "pytest>=7.0.0",
            "pytest-asyncio>=0.21.0",
            "pytest-timeout>=2.0.0",
            "httpx>=0.24.0",
        ],
    },
    python_requires=">=3.9",
)
