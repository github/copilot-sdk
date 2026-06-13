GitHub Copilot SDK for Python
==============================

.. toctree::
   :maxdepth: 2
   :caption: Contents

   api

The GitHub Copilot SDK for Python provides a JSON-RPC based client for
programmatic control of the GitHub Copilot CLI. It enables you to create
sessions, send messages, define tools, and handle events from the Copilot
agent loop.

Quick start
-----------

.. code-block:: python

   from copilot import CopilotClient

   async with CopilotClient() as client:
       session = await client.create_session()
       response = await session.send("Hello!")

Installation
------------

.. code-block:: bash

   pip install github-copilot-sdk

Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
