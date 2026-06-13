# Configuration file for the Sphinx documentation builder.
# https://www.sphinx-doc.org/en/master/usage/configuration.html

import importlib.metadata

project = "GitHub Copilot SDK for Python"
copyright = "2025, GitHub"
author = "GitHub"

try:
    release = importlib.metadata.version("github-copilot-sdk")
except importlib.metadata.PackageNotFoundError:
    release = "0.0.0.dev0"

# -- General configuration ---------------------------------------------------

extensions = [
    "sphinx.ext.autodoc",
    "sphinx.ext.napoleon",
    "sphinx.ext.intersphinx",
    "sphinx_autodoc_typehints",
]

# Napoleon settings (Google/NumPy style docstrings)
napoleon_google_docstring = True
napoleon_numpy_docstring = True
napoleon_include_init_with_doc = True

# autodoc settings
autodoc_member_order = "bysource"
autodoc_typehints = "description"
autodoc_class_signature = "separated"

# Intersphinx mapping
intersphinx_mapping = {
    "python": ("https://docs.python.org/3", None),
}

# -- Options for HTML output -------------------------------------------------

html_theme = "furo"
html_title = "GitHub Copilot SDK for Python"

# Exclude generated/internal modules from documentation
exclude_patterns = ["_build"]
