from setuptools import find_packages, setup

setup(
    name="github-copilot-sdk",
    version="0.1.0",
    packages=find_packages(),
    install_requires=[
        "typing-extensions>=4.0.0",
    ],
    python_requires=">=3.8",
)
