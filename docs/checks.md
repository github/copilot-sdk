All checks done by actionlint
=============================

This document describes all checks done by [actionlint](..) with example inputs, outputs, and playground links.

List of checks:

- [Unexpected keys](#check-unexpected-keys)
- [Missing required keys or key duplicates](#check-missing-required-duplicate-keys)
- [Unexpected empty mappings](#check-empty-mapping)
- [Unexpected mapping values](#check-mapping-values)
- [Syntax check for expression `${{ }}`](#check-syntax-expression)
- [Type checks for expression syntax in `${{ }}`](#check-type-check-expression)
- [Contexts and built-in functions](#check-contexts-and-builtin-func)
- [Contextual typing for `steps.<step_id>` objects](#check-contextual-step-object)
- [Contextual typing for `matrix` object](#check-contextual-matrix-object)
- [Contextual typing for `needs` object](#check-contextual-needs-object)
- [Strict type checks for comparison operators](#check-comparison-types)
- [shellcheck integration for `run:`](#check-shellcheck-integ)
- [pyflakes integration for `run:`](#check-pyflakes-integ)
- [Script injection by potentially untrusted inputs](#untrusted-inputs)
- [Job dependencies validation](#check-job-deps)
- [Matrix values](#check-matrix-values)
- [Webhook events validation](#check-webhook-events)
- [Workflow dispatch event validation](#check-workflow-dispatch-events)
- [Glob filter pattern syntax validation](#check-glob-pattern)
- [CRON syntax check at `schedule:`](#check-cron-syntax)
- [Runner labels](#check-runner-labels)
- [Action format in `uses:`](#check-action-format)
- [Local action inputs validation at `with:`](#check-local-action-inputs)
- [Popular action inputs validation at `with:`](#check-popular-action-inputs)
- [Outdated popular actions detection at `uses:`](#detect-outdated-popular-actions)
- [Shell name validation at `shell:`](#check-shell-names)
- [Job ID and step ID uniqueness](#check-job-step-ids)
- [Hardcoded credentials](#check-hardcoded-credentials)
- [Environment variable names](#check-env-var-names)
- [Permissions](#permissions)
- [Reusable workflows](#check-reusable-workflows)
- [ID naming convention](#id-naming-convention)
- [Contexts and special functions availability](#ctx-spfunc-availability)
- [Deprecated workflow commands](#check-deprecated-workflow-commands)
- [Conditions always evaluated to true at `if:`](#if-cond-always-true)
- [Action metadata syntax validation](#action-metadata-syntax)

Note that actionlint focuses on catching mistakes in workflow files. If you want some general code style checks, please consider
using a general YAML checker like [yamllint][].

<a id="check-unexpected-keys"></a>
## Unexpected keys

Example input:

```yaml
on: push
jobs:
  test:
    runs-on: ubuntu-latest
    # ERROR: Typo of `defaults:`
    default:
      run:
        working-directory: /path/to/dir
    steps:
      - run: echo hello
        # ERROR: `shell:` must be in lower case
        Shell: bash
```

Output:

```
test.yaml:6:5: unexpected key "default" for "job" section. expected one of "concurrency", "container", "continue-on-error", "defaults", "env", "environment", "if", "name", "needs", "outputs", "permissions", "runs-on", "secrets", "services", "steps", "strategy", "timeout-minutes", "uses", "with" [syntax-check]
  |
6 |     default:
  |     ^~~~~~~~
test.yaml:12:9: unexpected key "Shell" for "step" section. expected one of "continue-on-error", "env", "id", "if", "name", "run", "shell", "timeout-minutes", "uses", "with", "working-directory" [syntax-check]
   |
12 |         Shell: bash
   |         ^~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNo8jEEOAiEMRfdzin8Bwp5reAIYqqCEEtrGeHsDcVy1zXuv3AOGSTmenCQcgJLomsC0Lm5xS9bVXIuLbZTpHq39vG1eK/Dm+ar94XKddCrPT4AfUYtX9rnO7YnSkCtxuwedhVGoNf6/uq0zIEUp3wAAAP//tPsxjA==)

[Workflow syntax][syntax-doc] defines what keys can be defined in which mapping object. When unknown key is defined, it makes
the workflow run fail.

actionlint can detect unexpected keys while parsing workflow syntax and report them as an error.

Key names are basically case-sensitive (though some specific key names are case-insensitive). This check is useful to catch
case-sensitivity mistakes.

<a id="check-missing-required-duplicate-keys"></a>
## Missing required keys and key duplicates

Example input:

```yaml
on: push
jobs:
  test:
    strategy:
      # ERROR: Matrix name is duplicated. These keys are case-insensitive
      matrix:
        version_name: [v1, v2]
        VERSION_NAME: [V1, V2]
    # ERROR: runs-on is missing
    steps:
      - run: echo 'hello'
```

Output:

```
test.yaml:3:3: "runs-on" section is missing in job "test" [syntax-check]
  |
3 |   test:
  |   ^~~~~
test.yaml:8:9: key "VERSION_NAME" is duplicated in "matrix" section. previously defined at line:7,col:9. note that this key is case insensitive [syntax-check]
  |
8 |         VERSION_NAME: [V1, V2]
  |         ^~~~~~~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNo8zLEKwkAQhOE+TzFdmljEcjuLFBZGULgmSDhl8SLJbbjdHPr2Ihqr4eeDkUiYFw3FQ65KBWCs9llALXnj++tbwOQtDc+1gMxJB4l99BMTulxXyNvLn11zOu+Pbd/uDg2hc3UF92M1nnU92iAtkcC3ICgDj6OU7wAAAP//Nm8osg==)

Some mappings must include specific keys. For example, job mappings must include `runs-on:` and `steps:`.

And duplicate keys are not allowed. In workflow syntax, comparing some keys is **case-insensitive**. For example, the job ID
`test` in lower case and the job ID `TEST` in upper case are not able to exist in the same workflow.

actionlint checks these missing required keys and duplicate keys while parsing, and reports an error.

<a id="check-empty-mapping"></a>
## Unexpected empty mappings

Example input:

```yaml
on: push
jobs:
```

Output:

```
test.yaml:2:6: "jobs" section should not be empty. please remove this section if it's unnecessary [syntax-check]
  |
2 | jobs:
  |      ^
```

[Playground](https://rhysd.github.io/actionlint/#eNrKz7NSKCgtzuDKyk8qtgIEAAD//yULBOo=)

Some mappings and sequences should not be empty. For example, `steps:` must include at least one step.

actionlint checks such mappings and sequences are not empty while parsing, and reports the empty mappings and sequences as an
error.

<a id="check-mapping-values"></a>
## Unexpected mapping values

Example input:

```yaml
on: push
jobs:
  test:
    strategy:
      # ERROR: Boolean value "true" or "false" is expected
      fail-fast: off
      # ERROR: Integer value is expected
      max-parallel: 1.5
    runs-on: ubuntu-latest
    steps:
      - run: sleep 200
        # ERROR: Float value is expected
        timeout-minutes: two minutes
```

Output:

```
test.yaml:6:18: expecting a single ${{...}} expression or boolean literal "true" or "false", but found plain text node [syntax-check]
  |
6 |       fail-fast: off
  |                  ^~~
test.yaml:8:21: expected scalar node for integer value but found scalar node with "!!float" tag [syntax-check]
  |
8 |       max-parallel: 1.5
  |                     ^~~
test.yaml:13:26: expecting a single ${{...}} expression or float number literal, but found plain text node [syntax-check]
   |
13 |         timeout-minutes: two minutes
   |                          ^~~
```

[Playground](https://rhysd.github.io/actionlint/#eNo0zEEKAjEMheH9nOJdoDIKbnKbDKQ6kmlLk6DeXqp1Fd5P+GohtLD78qib0QK4mI8LmHd2ub1/C8i8a8psTqg5z3jwKzXurCpKOJ+u396jWBp0bFE8kvJgpyrN/mQanwRTkYbLus4M+H5IDU/HXsLFCP6smOMTAAD//2y5NWY=)

Some mapping values are restricted to some constant strings. Several mapping values expect boolean value like `true` or
`false`. And some mapping values expect integer or floating number values.

actionlint checks such constant strings are used properly while parsing and reports an error when an unexpected value is
specified.

<a id="check-syntax-expression"></a>
## Syntax check for expression `${{ }}`

Example input:

```yaml
on: push
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      # " is not available for string literal delimiter
      - run: echo '${{ "hello" }}'
      # + operator does not exist
      - run: echo '${{ 1 + 1 }}'
      # Missing ')' paren
      - run: echo "${{ toJson(hashFiles('**/lock', '**/cache/') }}"
      # unexpected end of input
      - run: echo '${{ github.event. }}'
```

Output:

```
test.yaml:7:24: got unexpected character '"' while lexing expression, expecting 'a'..'z', 'A'..'Z', '_', '0'..'9', ''', '}', '(', ')', '[', ']', '.', '!', '<', '>', '=', '&', '|', '*', ',', ' '. do you mean string literals? only single quotes are available for string delimiter [expression]
  |
7 |       - run: echo '${{ "hello" }}'
  |                        ^~~~~~~
test.yaml:9:26: got unexpected character '+' while lexing expression, expecting 'a'..'z', 'A'..'Z', '_', '0'..'9', ''', '}', '(', ')', '[', ']', '.', '!', '<', '>', '=', '&', '|', '*', ',', ' ' [expression]
  |
9 |       - run: echo '${{ 1 + 1 }}'
  |                          ^
test.yaml:11:65: unexpected end of input while parsing arguments of function call. expecting ",", ")" [expression]
   |
11 |       - run: echo "${{ toJson(hashFiles('**/lock', '**/cache/') }}"
   |                                                                 ^~~
test.yaml:13:38: unexpected end of input while parsing object property dereference like 'a.b' or array element dereference like 'a.*'. expecting "IDENT", "*" [expression]
   |
13 |       - run: echo '${{ github.event. }}'
   |                                      ^~~
```

[Playground](https://rhysd.github.io/actionlint/#eNp0zUGKwzAMheF9TvEwA85kJgnZ5gBd9BaJEXVaY4VK7ib47kXtOisJ/g8e5xl7kdjceZW5AZRE7QLPkqW3XtaStfRpsfZJorTLVwG9yRkUIsP/HAdcpJTYoVZ/Rib8YToBzoDyVTi3cZF42RJJ67tuTBwe/h/2hiVEGv0vanVnI7dNY1kHelHWwcbeAQAA///H30Qz)

actionlint lexes and parses expression in `${{ }}` following [the expression syntax document][expr-doc]. It can detect
many syntax errors like invalid characters, missing parentheses, unexpected end of input, ...

<a id="check-type-check-expression"></a>
## Type checks for expression syntax in `${{ }}`

actionlint checks types of expressions in `${{ }}` placeholders of templates. The following types are supported by the type
checker.

| Type          | Description                                                                                | Notation                 |
|---------------|--------------------------------------------------------------------------------------------|--------------------------|
| Any           | Any value like `any` type in TypeScript. Fallback type when a value can no longer be typed | `any`                    |
| Number        | Number value (integer or float)                                                            | `number`                 |
| Bool          | Boolean value                                                                              | `bool`                   |
| String        | String value                                                                               | `string`                 |
| Null          | Type of `null` value                                                                       | `null`                   |
| Array         | Array of specific type elements                                                            | `array<T>`               |
| Loose object  | Object which can contain any properties                                                    | `object`                 |
| Strict object | Object whose properties are strictly typed                                                 | `{prop1: T1, prop2: T2}` |
| Map object    | Object who has specific type values like `env` context                                     | `{string => T}`          |

Type check by actionlint is stricter than GitHub Actions runtime.

- Only `any` and `number` are allowed to be converted to string implicitly
- Implicit conversion to `number` is not allowed
- Object, array, and null are not allowed to be evaluated at `${{ }}`

Example input:

```yaml
on: push
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      # ERROR: `env` is object. Index access object is invalid
      - run: echo '${{ env[0] }}'
      # ERROR: Properties in objects are strongly typed. Missing property can be caught
      - run: echo '${{ job.container.os }}'
      # ERROR: `github.repository` is string. Trying to access .owner property is invalid
      - run: echo '${{ github.repository.owner }}'
      # ERROR: Objects, arrays and null should not be evaluated at ${{ }} since the outputs are useless
      - run: echo '${{ env }}'
```

Output:

```
test.yaml:7:28: property access of object must be type of string but got "number" [expression]
  |
7 |       - run: echo '${{ env[0] }}'
  |                            ^~
test.yaml:9:24: property "os" is not defined in object type {id: string; network: string} [expression]
  |
9 |       - run: echo '${{ job.container.os }}'
  |                        ^~~~~~~~~~~~~~~~
test.yaml:11:24: receiver of object dereference "owner" must be type of object but got "string" [expression]
   |
11 |       - run: echo '${{ github.repository.owner }}'
   |                        ^~~~~~~~~~~~~~~~~~~~~~~
test.yaml:13:20: object, array, and null values should not be evaluated in template with ${{ }} but evaluating the value of type {string => string} [expression]
   |
13 |       - run: echo '${{ env }}'
   |                    ^~~
```

[Playground](https://rhysd.github.io/actionlint/#eNp8yzEKwzAMheE9p3hDIVNMZ1+ldIiDqB2KZCwppYTcvbidm+kN//eEI6prHlZJGgfASK0v0Jx16t2Ts/n0nHv7JjWq+lPA1GUELVkwXvYdxNvtesdxjP/EKikswjYXphZEz+yjWPYUGlXRYtLeQV5M7exCvPX8CQAA//+nCkLw)

Type checks for expression syntax in `${{ }}` are done by semantics checker. Note that actual type checks by GitHub Actions
runtime is loose.

Any object value can be assigned into string value as string `'Object'`. `echo '${{ env }}'` will be replaced with
`echo 'Object'`. And an array can also be converted into `'Array'` string. Such loose conversions are bugs in almost all cases.
actionlint checks types more strictly. actionlint checks values evaluated at `${{ }}` are not object (replaced with string
`'Object'`), array (replaced with string `'Array'`), nor null (replaced with string `''`). If you want to check a content of
object or array, use `toJSON()` function.

```
echo '${{ toJSON(github.event) }}'
```

There are two object types internally. One is an object which is strict for properties, which causes a type error when trying to
access unknown properties. And another is an object which is not strict for properties, which allows accessing unknown properties.
In the case, accessing unknown property is typed as `any`.

When the type check cannot be done statically, the type is deduced to `any` (e.g. return type of `toJSON()`).

As special case of `${{ }}`, it can be used for expanding object and array values.

Example input:

```yaml
on: push
jobs:
  test:
    strategy:
      matrix:
        env_string:
          - 'FOO=BAR'
          - 'FOO=PIYO'
        env_object:
          - FOO: BAR
          - FOO: PIYO
    runs-on: ubuntu-latest
    steps:
      # OK: Expanding object at 'env:' section
      - run: echo "$FOO"
        env: ${{ matrix.env_object }}
      # ERROR: String value cannot be expanded as object
      - run: echo "$FOO"
        env: ${{ matrix.env_string }}
```

Output:

```
test.yaml:19:14: type of expression at "env" must be object but found type string [expression]
   |
19 |         env: ${{ matrix.env_string }}
   |              ^~~
```

[Playground](https://rhysd.github.io/actionlint/#eNqckL/KgzAUxXef4iCCU74HCHyDDkKnFLdORSX4hzaR5Ka0SN69pCpW3DqF+yPn5JerFcfobBcNurY8AkhaCidgyVQk29c8AfeKTP9cJ0Cqx9WS6VW7MYAhLYT4z7MyPdLz6SLSXYGuB9nQvqAQgiPPyiMM+Q81TlkW3F3tFDl2q4L3oi1HuxaycJNDNp1GnBRCxN+vcyTTtPzrb7OB97/F523A+3cAAAD//4EEWVQ=)

In above example, environment variables mapping is expanded at `env:` section. actionlint checks type of the expanded value.

<a id="check-contexts-and-builtin-func"></a>
## Contexts and built-in functions

Example input:

```yaml
on: push
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      # Access undefined context
      - run: echo '${{ unknown_context }}'
      # Access undefined property of context
      - run: echo '${{ github.events }}'
      # Calling undefined function (start's'With is correct)
      - run: echo "${{ startWith('hello, world', 'lo,') }}"
      # Wrong number of arguments
      - run: echo "${{ startsWith('hello, world') }}"
      # Wrong type of parameter
      - run: echo "${{ startsWith('hello, world', github.event) }}"
      # Function overloads can be handled properly. contains() has string version and array version
      - run: echo "${{ contains('hello, world', 'lo,') }}"
      - run: echo "${{ contains(github.event.labels.*.name, 'enhancement') }}"
      # format() has a special check for formatting string
      - run: echo "${{ format('{0}{1}', 1, 2, 3) }}"
```

Output:

```
test.yaml:7:24: undefined variable "unknown_context". available variables are "env", "github", "inputs", "job", "matrix", "needs", "runner", "secrets", "steps", "strategy", "vars" [expression]
  |
7 |       - run: echo '${{ unknown_context }}'
  |                        ^~~~~~~~~~~~~~~
test.yaml:9:24: property "events" is not defined in object type {action: string; action_path: string; action_ref: string; action_repository: string; action_status: string; actor: string; actor_id: string; api_url: string; base_ref: string; env: string; event: object; event_name: string; event_path: string; graphql_url: string; head_ref: string; job: string; job_workflow_sha: string; path: string; ref: string; ref_name: string; ref_protected: string; ref_type: string; repository: string; repository_id: string; repository_owner: string; repository_owner_id: string; repositoryurl: string; retention_days: number; run_attempt: string; run_id: string; run_number: string; secret_source: string; server_url: string; sha: string; token: string; triggering_actor: string; workflow: string; workflow_ref: string; workflow_sha: string; workspace: string} [expression]
  |
9 |       - run: echo '${{ github.events }}'
  |                        ^~~~~~~~~~~~~
test.yaml:11:24: undefined function "startWith". available functions are "always", "cancelled", "contains", "endswith", "failure", "format", "fromjson", "hashfiles", "join", "startswith", "success", "tojson" [expression]
   |
11 |       - run: echo "${{ startWith('hello, world', 'lo,') }}"
   |                        ^~~~~~~~~~~~~~~~~
test.yaml:13:24: number of arguments is wrong. function "startsWith(string, string) -> bool" takes 2 parameters but 1 arguments are given [expression]
   |
13 |       - run: echo "${{ startsWith('hello, world') }}"
   |                        ^~~~~~~~~~~~~~~~~~
test.yaml:15:51: 2nd argument of function call is not assignable. "object" cannot be assigned to "string". called function type is "startsWith(string, string) -> bool" [expression]
   |
15 |       - run: echo "${{ startsWith('hello, world', github.event) }}"
   |                                                   ^~~~~~~~~~~~~
test.yaml:20:24: format string "{0}{1}" does not contain placeholder {2}. remove argument which is unused in the format string [expression]
   |
20 |       - run: echo "${{ format('{0}{1}', 1, 2, 3) }}"
   |                        ^~~~~~~~~~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNqckMFOwzAQRO/9ilGF5IKciMItP8IROWHBAWe3yq4pUuR/Ry4SAqnNoScf5r3xaIU7HLLGzbv02m0AI7X6AnNmbWqe+8yWmxRqdorU6KA/FNBUsgMNUeBulgWZP1iO/DwIG30ZSnGX0LfRYu5b+iQ2vQBuK6gWZnsaLe5cpJTE4yhzenEeLol3tyhlu+rqGfk6y/9bvdpRLxBG1itG/6p/P2tT6Clpe9dymMjDEcfAA03Etl73KvMUbOeW+7Lsi/PYezx4PJ6k7wAAAP//nfWd6A==)

[Contexts][contexts-doc] and [built-in functions][funcs-doc] are strongly typed. Typos in property access of contexts and
function names can be checked. And invalid function calls like wrong number of arguments or type mismatch at parameter also
can be checked thanks to type checker.

The semantics checker can properly handle that

- some functions are overloaded (e.g. `contains(str, substr)` and `contains(array, item)`)
- some parameters are optional (e.g. `join(strings, sep)` and `join(strings)`)
- some parameters are repeatable (e.g. `hashFiles(file1, file2, ...)`)

In addition, `format()` function has a special check for placeholders in the first parameter which represents the formatting
string.

Note that context names and function names are case-insensitive. For example, `toJSON` and `toJson` are the same function.

<a id="check-contextual-step-object"></a>
## Contextual typing for `steps.<step_id>` objects

Example input:

```yaml
on: push
jobs:
  test:
    runs-on: ubuntu-latest
    outputs:
      # Step outputs can be used in job outputs since this section is evaluated after all steps were run
      foo: '${{ steps.get_value.outputs.name }}'
    steps:
      # ERROR: Access undefined step outputs
      - run: echo '${{ steps.get_value.outputs.name }}'
      # Outputs are set here
      - run: echo "foo=value" >> "$GITHUB_OUTPUT"
        id: get_value
      # OK
      - run: echo '${{ steps.get_value.outputs.name }}'
      # OK
      - run: echo '${{ steps.get_value.conclusion }}'
  other:
    runs-on: ubuntu-latest
    steps:
      # ERROR: Access undefined step outputs. Step objects are job-local
      - run: echo '${{ steps.get_value.outputs.name }}'
```

Output:

```
test.yaml:10:24: property "get_value" is not defined in object type {} [expression]
   |
10 |       - run: echo '${{ steps.get_value.outputs.name }}'
   |                        ^~~~~~~~~~~~~~~~~~~~~~~~~~~~
test.yaml:22:24: property "get_value" is not defined in object type {} [expression]
   |
22 |       - run: echo '${{ steps.get_value.outputs.name }}'
   |                        ^~~~~~~~~~~~~~~~~~~~~~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNqskDFrhEAQhXt/xWMRrPQHLMQiTZIqKbQWNWs0mB1xZtKI/z3sxgQODo7jrprifd88eOQtFuUx+aSObQKIYwkXWNVzHnLt1IvmcxuyGJHKosK/HDAQWWTptoHFLVx8OGm+21ldcYCFb78c9j2LQoT+3Dz0WLh+pCtenHpmIHqItEFZwqRPL9Vz/di81tVbXZnDAKZ3i//Hd+w/6/Xk+1l5In9YJKNbL0572zg/AQAA//8kVZJl)

Outputs of step can be accessed via `steps.<step_id>` objects. The `steps` context is dynamic:

- Accessing the outputs before running the step causes `null`
- Outputs of steps only in the job can be accessed. It cannot access steps across jobs

It is a common mistake to access the wrong step outputs since people often forget to fix placeholders on copying&pasting
steps. actionlint can catch invalid accesses to step outputs and reports them as errors.

When the outputs are set by popular actions, the outputs object is more strictly typed.

Example input:

```yaml
on: push

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      # ERROR: The step is not run yet at this point
      - run: echo ${{ steps.cache.outputs.cache-hit }}
      # actions/cache sets cache-hit output
      - uses: actions/cache@v4
        id: cache
        with:
          key: ${{ hashFiles('**/*.lock') }}
          path: ./packages
      # OK
      - run: echo ${{ steps.cache.outputs.cache-hit }}
      # ERROR: Typo at output name
      - run: echo ${{ steps.cache.outputs.cache_hit }}
```

Output:

```
test.yaml:8:23: property "cache" is not defined in object type {} [expression]
  |
8 |       - run: echo ${{ steps.cache.outputs.cache-hit }}
  |                       ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~
test.yaml:18:23: property "cache_hit" is not defined in object type {cache-hit: string} [expression]
   |
18 |       - run: echo ${{ steps.cache.outputs.cache_hit }}
   |                       ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNqkjsFKxDAQhu99iv8grBbaXjzNyZOvIdk4OLElCc6MIsu+uyRd6lUwlzDzfT//lEyorjIM7+WsNADGau0HPjzr1AQ/ezafttBYR2pcdbeAqZkEjlJwd7nscI4hCs/FrbrdpkmS4Xo9Yq6shBAtlaxLV54+H28YSK+Evjw2X8mEjglY+Zt6pQSV57Sx3p/GcRnnrcT19PDb1V4NJoR5qSGu4Y31v9f/Mfayx34CAAD//4ltbNw=)

In the above example, [actions/cache][actions-cache] action sets `cache-hit` output so that the following steps can know
whether the cache was hit or not. At line 8, the cache action is not run yet. So `cache` property does not exist in the
`steps` context yet. On running the step whose ID is `cache`, `steps.cache` object is typed as
`{outputs: {cache-hit: any}, conclusion: string, outcome: string}`. At line 18, the expression has a typo in the output
name. actionlint can check it because properties of `steps.cache.outputs` are typed.

This strict typing for outputs is also applied to local actions. Let's say we have the following local action.

```yaml
name: 'My action with output'
author: 'rhysd <https://rhysd.github.io>'
description: 'my action with outputs'

outputs:
  some_value:
    description: some value returned from this action

runs:
  using: 'node20'
  main: 'index.js'
```

Example input:

```yaml
on: push

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      # ERROR: The step is not yet run
      - run: echo ${{ steps.my_action.outputs.some_value }}
      # The action runs here and sets its outputs
      - uses: ./.github/actions/my-action-with-output
        id: my_action
      # OK
      - run: echo ${{ steps.my_action.outputs.some_value }}
      # ERROR: No output named 'some-value' (typo)
      - run: echo ${{ steps.my_action.outputs.some-value }}
```

Output:
<!-- Skip update output -->

```
test.yaml:8:23: property "my_action" is not defined in object type {} [expression]
  |
8 |       - run: echo ${{ steps.my_action.outputs.some_value }}
  |                       ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
test.yaml:15:23: property "some-value" is not defined in object type {some_value: string} [expression]
   |
15 |       - run: echo ${{ steps.my_action.outputs.some-value }}
   |                       ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
```

<!-- Skip playground link -->

The 'My action with output' action defines one output `some_value`. The property is typed at `steps.my_action.outputs` object
so that actionlint can check incorrect property accesses like a typo in the output name.

<a id="check-contextual-matrix-object"></a>
## Contextual typing for `matrix` object

Example input:

```yaml
on: push
jobs:
  test:
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest]
        node: [14, 15]
        package:
          - name: 'foo'
            optional: true
          - name: 'bar'
            optional: false
        include:
          - node: 15
            npm: 7.5.4
    runs-on: ${{ matrix.os }}
    steps:
      # Access undefined matrix value
      - run: echo '${{ matrix.platform }}'
      # Matrix value is strongly typed. Below line causes an error since matrix.package is {name: string, optional: bool}
      - run: echo '${{ matrix.package.dev }}'
      # OK
      - run: |
          echo 'os: ${{ matrix.os }}'
          echo 'node version: ${{ matrix.node }}'
          echo 'package: ${{ matrix.package.name }} (optional=${{ matrix.package.optional }})'
      # Additional matrix values in 'include:' are supported
      - run: echo 'npm version is specified'
        if: ${{ contains(matrix.npm, '7.5') }}
  test2:
    runs-on: ubuntu-latest
    steps:
      # Matrix values in other job is not accessible
      - run: echo '${{ matrix.os }}'
```

Output:

```
test.yaml:19:24: property "platform" is not defined in object type {node: number; npm: string; os: string; package: {name: string; optional: bool}} [expression]
   |
19 |       - run: echo '${{ matrix.platform }}'
   |                        ^~~~~~~~~~~~~~~
test.yaml:21:24: property "dev" is not defined in object type {name: string; optional: bool} [expression]
   |
21 |       - run: echo '${{ matrix.package.dev }}'
   |                        ^~~~~~~~~~~~~~~~~~
test.yaml:34:24: property "os" is not defined in object type {} [expression]
   |
34 |       - run: echo '${{ matrix.os }}'
   |                        ^~~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNqMUk1v6yAQvOdXzOFJJFJsKU+xIiG9XxK9A7FxQmsDYiFplfLfK+JQ58NVe0K7zCyzMxjNYQMdZi9mR3wGeEk+nQB5J7zcvw8V0Avv1FuuAEMc27AL2oeiE4m3xEnpxpzoWv//wmrTSI7tar3EqhrbVtSvYi/HmUABLXrJwVpj2E0fMNYro0XH4V2QU5SdcN9RWtHRyFG67kLz+O5F46q6m6Btz7Epq3J9abugqUie/Tmfr4aUhhDj1TFpKQ8tEphD1gcDdgO3nfCtcT1iZD9BB3vKRh6f0R83OgdmCuRRF3tCpTVxlI7Uwx6XiylGTgkT0pLziBHzbPW/CVC+Q4yLyZ217bMkKAJZWatWyWbUotrh+dpoL5SmeRZt+yXYpqzYYkgh/bu//D6su0/6+6QGAz8DAAD//6xO5N4=)

Types of `matrix` context are contextually checked by the semantics checker. Type of matrix values in `matrix:` section
is deduced from element values of its array. When the matrix value is an array of objects, objects' properties are checked
strictly like `package.name` in above example.

When a type of the array elements is not persistent, the type of the matrix value falls back to `any`.

```yaml
strategy:
  matrix:
    foo:
      - 'string value'
      - 42
      - {aaa: true, bbb: null}
    bar:
      - [42]
      - [true]
      - [{aaa: true, bbb: null}]
      - []
steps:
  # matrix.foo is any type value
  - run: echo ${{ matrix.foo }}
  # matrix.bar is array<any> type value
  - run: echo ${{ matrix.bar[0] }}
  # ERROR: Array cannot be evaluated as string
  - run: echo ${{ matrix.bar }}
```

<a id="check-contextual-needs-object"></a>
## Contextual typing for `needs` object

Example input:

```yaml
on: push
jobs:
  install:
    outputs:
      installed: '...'
    runs-on: ubuntu-latest
    steps:
      - run: echo 'install something'
  prepare:
    outputs:
      prepared: '...'
    runs-on: ubuntu-latest
    steps:
      - run: echo 'parepare something'
      # ERROR: Outputs in other job is not accessible
      - run: echo '${{ needs.prepare.outputs.prepared }}'
  build:
    needs: [install, prepare]
    outputs:
      built: '...'
    runs-on: ubuntu-latest
    steps:
      # OK: Accessing job results
      - run: echo 'build something with ${{ needs.install.outputs.installed }} and ${{ needs.prepare.outputs.prepared }}'
      # ERROR: Accessing undefined output causes an error
      - run: echo '${{ needs.install.outputs.foo }}'
      # ERROR: Accessing undefined job ID
      - run: echo '${{ needs.some_job }}'
  other:
    runs-on: ubuntu-latest
    steps:
      # ERROR: Cannot access outputs across jobs
      - run: echo '${{ needs.build.outputs.built }}'
```

Output:

```
test.yaml:16:24: property "prepare" is not defined in object type {} [expression]
   |
16 |       - run: echo '${{ needs.prepare.outputs.prepared }}'
   |                        ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
test.yaml:26:24: property "foo" is not defined in object type {installed: string} [expression]
   |
26 |       - run: echo '${{ needs.install.outputs.foo }}'
   |                        ^~~~~~~~~~~~~~~~~~~~~~~~~
test.yaml:28:24: property "some_job" is not defined in object type {install: {outputs: {installed: string}; result: string}; prepare: {outputs: {prepared: string}; result: string}} [expression]
   |
28 |       - run: echo '${{ needs.some_job }}'
   |                        ^~~~~~~~~~~~~~
test.yaml:33:24: property "build" is not defined in object type {} [expression]
   |
33 |       - run: echo '${{ needs.build.outputs.built }}'
   |                        ^~~~~~~~~~~~~~~~~~~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNqkkUFOxDAMRfdzir9A6ob2ALkKQqglhnRU4qh2xGLUuyNnMqmAESBmUamu7f/+dzk6pCzhcORJ3AGYo+i4LPYKcNaUVc5F65F36IZh6MrnNUfpTSZPOWrul1FJtLREKbXl3iYd6DkwuqoE4TfSMMdX00orpXGlq+jau51sKvZ8Rn8buzudEIm8DJU8VEOX2mPbbHPK8+LPpDLv8FDD3V9MP14LZHt6c5pC36PgfdaA3Xt10ry3H4htwxg9/hzzhwN9hbww/75klp+OPNVJ1kCr+/8dduFykealXNkYHwEAAP//zBzrFQ==)

Job dependencies can be defined at [`needs:`][needs-doc]. A job runs after all jobs defined in `needs:` are done.
Outputs from the jobs can be accessed only from jobs following them via [`needs` context][needs-context-doc].

actionlint defines a type of `needs` variable contextually by looking at each job's `outputs:` section and `needs:` section.

<a id="check-comparison-types"></a>
## Strict type checks for comparison operators

Example input:

```yaml
on:
  workflow_call:
    inputs:
      timeout:
        type: boolean

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - run: echo 'called!'
        # ERROR: Comparing string to object is always evaluated to false
        if: ${{ github.event == 'workflow_call' }}
      - run: echo 'timeout is too long'
        # ERROR: Comparing boolean value with `>` doesn't make sense
        if: ${{ inputs.timeout > 60 }}
```

Output:

```
test.yaml:13:17: "object" value cannot be compared to "string" value with "==" operator [expression]
   |
13 |         if: ${{ github.event == 'workflow_call' }}
   |                 ^~~~~~~~~~~~
test.yaml:16:17: "bool" value cannot be compared to "number" value with ">" operator [expression]
   |
16 |         if: ${{ inputs.timeout > 60 }}
   |                 ^~~~~~~~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNpsj81KwDAQhO99ihGEnFo8eQjUV5Gmbtto3C3NxiIl7y7pH4jednc+ZmaFbQWssnwMQdbXvguhHADPc9J4zID6T5Kk1wro90wWTiRQx1X1Lm5nleIJLYljLWyRXGJNdeiKtktRab6d60JaUD8JTImntwdzx/jB4nHbMHqdkmvoi1jRtjC/Ghvk/J/d2Ro+QkUQhMe/1sejzcW+4PkJOf8EAAD//0dGU1w=)

Expressions in `${{ }}` placeholders support `==`, `!=`, `>`, `>=`, `<`, `<=` comparison operators. Arbitrary types of operands
can be compared. When different type values are compared, they are implicitly converted to numbers before the comparison. Please
see [the official document][operators-doc] to know the details of operators behavior.

However, comparisons between some types are actually meaningless:

- Objects and arrays are converted to `NaN`. Comparing an object or an array with other type is always evaluated to false.
- Comparing booleans, null, objects, and arrays with `>`, `>=`, `<`, `<=` makes no sense.

actionlint checks operands of comparison operators and reports errors in these cases.

There are some additional surprising behaviors, but actionlint allows them not to cause false positives as much as possible.

- `0 == null`, `'0' == null`, `false == null` are true since they are implicitly converted to `0 == 0`
- `'0' == false` and `0 == false` are true due to the same reason as above
- Objects and arrays are only considered equal when they are the same instance

<a id="check-shellcheck-integ"></a>
## [shellcheck][] integration for `run:`

Example input:

```yaml
on: push
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - run: echo $FOO
  test-win:
    runs-on: windows-latest
    steps:
      # Shell on Windows is PowerShell by default.
      # shellcheck is not run in this case.
      - run: echo $FOO
      # This script is run with bash due to 'shell:' configuration
      - run: echo $FOO
        shell: bash
```

Output:

```
test.yaml:6:9: shellcheck reported issue in this script: SC2086:info:1:6: Double quote to prevent globbing and word splitting [shellcheck]
  |
6 |       - run: echo $FOO
  |         ^~~~
test.yaml:14:9: shellcheck reported issue in this script: SC2086:info:1:6: Double quote to prevent globbing and word splitting [shellcheck]
   |
14 |       - run: echo $FOO
   |         ^~~~
```

<!-- Skip playground link -->

[shellcheck][] is a famous linter for ShellScript. actionlint runs shellcheck for scripts at `run:` step in a workflow.
For installing shellcheck, see [the official installation document][shellcheck-install].

actionlint detects which shell is used to run the scripts following [the documentation][shell-doc]. On Linux or macOS the
default shell is `bash`, and on Windows it is `pwsh`. Shell can be configured by `shell:` configuration at a workflow
level or job level. Each step can configure shell to run scripts by `shell:`.

In the above example output, `SC2086:info:1:6:` means that shellcheck reported SC2086 rule violation and the location is at
line 1, column 6. Note that the location is relative to the script of the `run:` section.

actionlint remembers the default shell and checks what OS the job runs on. Only when the shell is `bash` or `sh`, actionlint
applies shellcheck to scripts.

By default, actionlint checks if `shellcheck` command exists in your system and uses it when it is found. The `-shellcheck`
option on running `actionlint` command specifies the executable path of shellcheck. Setting empty string by `shellcheck=`
disables shellcheck integration explicitly.

Since both `${{ }}` expression syntax and ShellScript's variable access `$FOO` use `$`, the remaining `${{ }}` confuses
shellcheck. To avoid it, actionlint replaces `${{ }}` with underscores. For example `echo '${{ matrix.os }}'` is replaced
with `echo '________________'`.

Some shellcheck rules conflict with the `${{ }}` expression syntax. To avoid errors due to the syntax, [SC1091][], [SC2050][],
[SC2194][], [SC2154][], [SC2157][], [SC2043][] are disabled.

When what shell is used cannot be determined statically, actionlint assumes `shell: bash` optimistically. For example,

```yaml
strategy:
  matrix:
    os: [ubuntu-latest, macos-latest, windows-latest]
runs-on: ${{ matrix.os }}
steps:
  - name: Show file content
    run: Get-Content -Path xxx\yyy.txt
    if: ${{ matrix.os == 'windows-latest' }}
```

The 'Show file content' script is only run by `pwsh` due to `matrix.os == 'windows-latest'` guard. However, actionlint does not
know that. It checks the script with shellcheck and it'd probably cause a false-positive (due to file separator). This kind of
false positives can be avoided by showing the shell name explicitly. It is also better in terms of maintenance of the workflow.

```yaml
- name: Show file content
  run: Get-Content -Path xxx\yyy.txt
  if: ${{ matrix.os == 'windows-latest' }}
  shell: pwsh
```

When you want to control shellcheck behavior, [`SHELLCHECK_OPTS` environment variable][shellcheck-env-var] is useful.

From command line:

```sh
# Enable some optional rules
SHELLCHECK_OPTS='--enable=avoid-nullary-conditions' actionlint

# Disable some rules
SHELLCHECK_OPTS='--exclude=SC2129' actionlint
```

On GitHub Actions:

```yaml
- run: actionlint
  env:
    SHELLCHECK_OPTS: --exclude=SC2129
```

<a id="check-pyflakes-integ"></a>
## [pyflakes][] integration for `run:`

Example input:

```yaml
on: push
jobs:
  linux:
    runs-on: ubuntu-latest
    steps:
      # Yay! No error
      - run: print('${{ runner.os }}')
        shell: python
      # ERROR: Undefined variable
      - run: print(hello)
        shell: python
  linux2:
    runs-on: ubuntu-latest
    defaults:
      run:
        # Run script with Python by default
        shell: python
    steps:
      - run: |
          import sys
          for sys in ['system1', 'system2']:
            print(sys)
      - run: |
          from time import sleep
          print(100)
```

Output:

```
test.yaml:10:9: pyflakes reported issue in this script: 1:7: undefined name 'hello' [pyflakes]
   |
10 |       - run: print(hello)
   |         ^~~~
test.yaml:19:9: pyflakes reported issue in this script: 2:5: import 'sys' from line 1 shadowed by loop variable [pyflakes]
   |
19 |       - run: |
   |         ^~~~
test.yaml:23:9: pyflakes reported issue in this script: 1:1: 'time.sleep' imported but unused [pyflakes]
   |
23 |       - run: |
   |         ^~~~
```

<!-- Skip playground link -->

Python script can be written in `run:` when `shell: python` is configured.

[pyflakes][] is a famous linter for Python. It is suitable for linting small code like scripts at `run:` since it focuses
on finding mistakes (not a code style issue) and tries to make false positives as minimal as possible. Install pyflakes
by `pip install pyflakes`.

actionlint runs pyflakes for scripts at `run:` steps in a workflow and reports errors found by pyflakes. actionlint detects
Python scripts in a workflow by checking `shell: python` at each step and `defaults:` configurations at workflows and jobs.

By default, actionlint checks if `pyflakes` command exists in your system and uses it when found. The `-pyflakes` option
of `actionlint` command allows to specify the executable path of pyflakes. Setting empty string by `pyflakes=` disables
pyflakes integration explicitly.

Since both `${{ }}` expression syntax is invalid as Python, remaining `${{ }}` might confuse pyflakes. To avoid it,
actionlint replaces `${{ }}` with underscores. For example `print('${{ matrix.os }}')` is replaced with
`print('________________')`.

<a id="untrusted-inputs"></a>
## Script injection by potentially untrusted inputs

Example input:

```yaml
name: Test
on: pull_request

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - name: Print pull request title
        # ERROR: Using the potentially untrusted input can cause script injection
        run: echo '${{ github.event.pull_request.title }}'
      - uses: actions/stale@v9
        with:
          repo-token: ${{ secrets.TOKEN }}
          # This is OK because action input is not evaluated by shell
          stale-pr-message: ${{ github.event.pull_request.title }} was closed
      - uses: actions/github-script@v7
        with:
          # ERROR: Using the potentially untrusted input can cause script injection
          script: console.log('${{ github.event.head_commit.author.name }}')
      - name: Get comments
        # ERROR: Accessing untrusted inputs via `.*` object filter; bodies of comment, review, and review_comment
        run: echo '${{ toJSON(github.event.*.body) }}'
      - name: Do something with checking skip
        # OK: This placeholder uses an untrusted input, but the input cannot be injected to the script
        run: if [ "${{ contains(github.event.pull_request.author.title, '[SKIP]') }}" = "true" ]; then echo "skip"; fi
```

Output:

```
test.yaml:10:24: "github.event.pull_request.title" is potentially untrusted. avoid using it directly in inline scripts. instead, pass it through an environment variable. see https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions for more details [expression]
   |
10 |         run: echo '${{ github.event.pull_request.title }}'
   |                        ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
test.yaml:19:36: "github.event.head_commit.author.name" is potentially untrusted. avoid using it directly in inline scripts. instead, pass it through an environment variable. see https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions for more details [expression]
   |
19 |           script: console.log('${{ github.event.head_commit.author.name }}')
   |                                    ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
test.yaml:22:31: object filter extracts potentially untrusted properties "github.event.comment.body", "github.event.discussion.body", "github.event.issue.body", "github.event.pull_request.body", "github.event.review.body", "github.event.review_comment.body". avoid using the value directly in inline scripts. instead, pass the value through an environment variable. see https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions for more details [expression]
   |
22 |         run: echo '${{ toJSON(github.event.*.body) }}'
   |                               ^~~~~~~~~~~~~~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNqEkUFr20AQhe/+FQ9RsFMq9Vi6oZBDS2kDSSC5hRDW64l369WOujPrUIL/e1nJmLjF5CRG8+Z7T0/J9mRwR6IzTgZDifEx0+9SX8x+8VLMDFASrU8glyRtFZZlSVraaOtuXInSIJMKaDGBb3JIOlKxp0KDRtrLRqABOc+Yv3t5wTqoL8uOtpS0ex2mG8+w280PDkVIDKzTwEk+itpIF9vPB/JzUG8OE5Bp4FZ5Q8mgWgm5TCrd3fXltyvsdq+kI6sdctuTiF3TdPB2NjxbgYsstDqRcmK04nIY9GL76WTaSWHgOAlH6iKvF/835MmuHh33fdDOFvWcu9p7rensnz/xnRRVSUnlVPvKP2+vrxZHFu+7Ja/+nB01PxG/MoR7Uh/SeswP58lt6iSbMBybhCfco6kmjpPakGRxus/9p4y1fsD8/vbyx83DvGZo8AWN5kINHs6hntIUv6mOzTmewt8AAAD//6j/5EY=)

Since `${{ }}` placeholders are evaluated and replaced directly by GitHub Actions runtime, you need to use them carefully in
inline scripts at `run:`. For example, if we have step as follows,

```yaml
- run: echo 'issue ${{github.event.issue.title}}'
```

an attacker can create a new issue with the title `'; malicious_command ...`, and the inline script will run
`echo 'issue'; malicious_command ...` in your workflow. The remediation of such script injection is passing potentially untrusted
inputs via environment variables. See [the official document][security-doc] for more details.

```yaml
- run: echo "issue ${TITLE}"
  env:
    TITLE: ${{github.event.issue.title}}
```

actionlint recognizes the following inputs as potentially untrusted and checks your inline scripts at `run:`. When they are used
directly in a script, actionlint will report it as an error.

- `github.event.issue.title`
- `github.event.issue.body`
- `github.event.pull_request.title`
- `github.event.pull_request.body`
- `github.event.comment.body`
- `github.event.review.body`
- `github.event.review_comment.body`
- `github.event.pages.*.page_name`
- `github.event.commits.*.message`
- `github.event.head_commit.message`
- `github.event.head_commit.author.email`
- `github.event.head_commit.author.name`
- `github.event.commits.*.author.email`
- `github.event.commits.*.author.name`
- `github.event.pull_request.head.ref`
- `github.event.pull_request.head.label`
- `github.event.pull_request.head.repo.default_branch`
- `github.head_ref`

Not only direct access to the untrusted properties, actionlint also detects those properties indirectly accessed via
[object filter syntax][object-filter-syntax]. For example, `github.event.*.body` collects all `body` properties in child objects
of `github.event` as array. Those properties include untrusted inputs like `github.event.comment.body`,
`github.event.pull_request.body`, ...

```sh
# Echo list of github.event.comment.body, github.event.pull_request.body, ...
echo '${{ toJSON(github.event.*.body) }}'
```

Instead, you should store the JSON string in an environment variable:

```sh
- run: echo "${BODIES}"
  env:
    BODIES: '${{ toJSON(github.event.*.body) }}'
```

The following functions return a boolean value so it is not possible to inject anything as the result of the returned value.
actionlint does not report an error even if untrusted inputs are passed to these function calls.

- `contains()`
- `startswith()`
- `endswith()`

At last, the popular action [actions/github-script][github-script] has the same issue in its `script` input. actionlint also
checks the input.

<a id="check-job-deps"></a>
## Job dependencies validation

Example input:

```yaml
on: push
jobs:
  prepare:
    needs: [build]
    runs-on: ubuntu-latest
    steps:
      - run: echo 'prepare'
  install:
    needs: [prepare]
    runs-on: ubuntu-latest
    steps:
      - run: echo 'install'
  build:
    needs: [install]
    runs-on: ubuntu-latest
    steps:
      - run: echo 'build'
```

Output:

```
test.yaml:3:3: cyclic dependencies in "needs" job configurations are detected. detected cycle is "prepare" -> "build" -> "install" -> "prepare" [job-needs]
  |
3 |   prepare:
  |   ^~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNqkjjEOwyAMRXdO8TcmLsBVqg7QWEoqZBC2719BvWTOZvn5v+/OGcPkDN9eJQdgTBpl0hoBJjok41Xtasd7r6axpJWyaqyWWlES3UiUhvyDQFqXGfQ5O6JLYwAuFi2t3f3OHzS4djXsZ+9+pw/8Wxp/AQAA//+/J1vk)

Job dependencies can be defined at [`needs:`][needs-doc]. If cyclic dependencies exist, jobs never start to run. actionlint
detects cyclic dependencies in `needs:` sections of jobs and reports it as an error.

actionlint also detects undefined jobs and duplicate jobs in `needs:` section.

Example input:

```yaml
on: push
jobs:
  foo:
    needs: [bar, BAR]
    runs-on: ubuntu-latest
    steps:
      - run: echo 'hi'
  bar:
    needs: [unknown]
    runs-on: ubuntu-latest
    steps:
      - run: echo 'hi'
```

Output:

```
test.yaml:4:18: job ID "BAR" duplicates in "needs" section. note that job ID is case insensitive [job-needs]
  |
4 |     needs: [bar, BAR]
  |                  ^~~~
test.yaml:8:3: job "bar" needs job "unknown" which does not exist in this workflow [job-needs]
  |
8 |   bar:
  |   ^~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNqkjDsOAjEMRPucYrptyAXcwRFoEUUMRuEjexXb4vooS0VNNdLMvGdKWNN7eRg7FeBmNgNQkasTTtzGDof98by1I9XrhJJTI+urhXhsk4es/mWBOp8EuXTD0u9LAbiNX3PqU+2t/4k/AQAA//96DTh7)

<a id="check-matrix-values"></a>
## Matrix values

Example input:

```yaml
on: push
jobs:
  test:
    strategy:
      matrix:
        node: [10, 12, 14, 14]
        os: [ubuntu-latest, macos-latest]
        exclude:
          - node: 13
            os: ubuntu-latest
          - node: 10
            platform: ubuntu-latest
    runs-on: ${{ matrix.os }}
    steps:
      - run: echo ...
```

Output:

```
test.yaml:6:28: duplicate value "14" is found in matrix "node". the same value is at line:6,col:24 [matrix]
  |
6 |         node: [10, 12, 14, 14]
  |                            ^~~
test.yaml:9:19: value "13" in "exclude" does not match in matrix "node" combinations. possible values are "10", "12", "14", "14" [matrix]
  |
9 |           - node: 13
  |                   ^~
test.yaml:12:13: "platform" in "exclude" section does not exist in matrix. available matrix configurations are "node", "os" [matrix]
   |
12 |             platform: ubuntu-latest
   |             ^~~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNpskMGOhCAQRO9+RR32KER298SvmD2gsuNMlDY0JE6M/z4h4jgmHggpqHrpanIaU+S+eFDDugCC5ZBugIM3wd6emwJGE/x93hXgqLMatapKqO8S6jedv/c3sUYdm+hCFINJ2BKjaYmzOpx2bofY2YMMiExXPx+PG/OEvIpUp8g0mPBPfrwK+uhYpA18LUuuJ4mxrrm/nXgfSiSzhm17gpTyFQAA//9UwlNB)

[`matrix:`][matrix-doc] defines combinations of multiple values. Nested `include:` and `exclude:` can add/remove specific
combination of matrix values. actionlint checks

- values in `exclude:` appear in `matrix:` or `include:`
- duplicate variations of matrix values

<a id="check-webhook-events"></a>
## Webhook events validation

Example input:

```yaml
on:
  push:
    # ERROR: Incorrect filter. 'branches' is correct
    branch: foo
    # ERROR: Both 'paths' and 'paths-ignore' filters cannot be used for the same event
    paths: path/to/foo
    paths-ignore: path/to/foo
  issues:
    # ERROR: Incorrect type. 'opened' is correct
    types: created
  release:
    # ERROR: 'tags' filter is not available for 'release' event
    tags: v*.*.*
  # ERROR: Unknown event name
  pullreq:

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - run: echo ...
```

Output:

```
test.yaml:4:5: unexpected key "branch" for "push" section. expected one of "branches", "branches-ignore", "paths", "paths-ignore", "tags", "tags-ignore", "types", "workflows" [syntax-check]
  |
4 |     branch: foo
  |     ^~~~~~~
test.yaml:7:5: both "paths" and "paths-ignore" filters cannot be used for the same event "push". note: use '!' to negate patterns [events]
  |
7 |     paths-ignore: path/to/foo
  |     ^~~~~~~~~~~~~
test.yaml:10:12: invalid activity type "created" for "issues" Webhook event. available types are "assigned", "closed", "deleted", "demilestoned", "edited", "labeled", "locked", "milestoned", "opened", "pinned", "reopened", "transferred", "unassigned", "unlabeled", "unlocked", "unpinned" [events]
   |
10 |     types: created
   |            ^~~~~~~
test.yaml:13:5: "tags" filter is not available for release event. it is only for push event [events]
   |
13 |     tags: v*.*.*
   |     ^~~~~
test.yaml:15:3: unknown Webhook event "pullreq". see https://docs.github.com/en/actions/learn-github-actions/events-that-trigger-workflows#webhook-events for list of all Webhook event names [events]
   |
15 |   pullreq:
   |   ^~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNpcjkGuwyAMRPecYtaRIHvfhuT7h1QIU2wq9fYVJJt2NbLf6NlSyAG1axoJbC2WPRH+ReZcoyWlGavJ+rX251Gk8S89VTvrpbN3ZSXsjaPxnwMaZ47KN42HEl5LWMIyv8i58ZOce8g2BcZqV7X1ol4KoW+9WPc5DjaRGtf7HOBHk8B7EoQQPgEAAP//dVRCRg==)

At `on:`, Webhook events can be specified to trigger the workflow. [Webhook event documentation][webhook-doc] defines
which Webhook events are available and what types can be specified at `types:` for each event.

actionlint validates the Webhook configurations:

- Webhook event name
- types for Webhook event
- filter names
- filter usages
  - `paths` and `paths-ignore`, `branches` and `branches-ignore`, `tags` and `tags-ignore` are exclusive. They can not
    be used for the same event.
  - Some filters are only available for specific events as explained in [the official document][specific-paths-doc]
    (see the following table).

| Filter name       | Events where the filter is available                                         |
|-------------------|------------------------------------------------------------------------------|
| `paths`           | `push`, `pull_request`, `pull_request_target`                                |
| `paths-ignore`    | `push`, `pull_request`, `pull_request_target`                                |
| `branches`        | `merge_group`, `push`, `pull_request`, `pull_request_target`, `workflow_run` |
| `branches-ignore` | `merge_group`, `push`, `pull_request`, `pull_request_target`, `workflow_run` |
| `tags`            | `push`                                                                       |
| `tags-ignore`     | `push`                                                                       |

The table of available Webhooks and their types are defined in [`all_webhooks.go`](../all_webhooks.go). It is generated
by [a script][generate-webhook-events] and kept to the latest by CI workflow triggered weekly.

<a id="check-workflow-dispatch-events"></a>
## Workflow dispatch event validation

Example input:

```yaml
on:
  workflow_dispatch:
    inputs:
      # Unknown input type
      id:
        type: text
      # ERROR: No options for 'choice' input type
      kind:
        type: choice
      name:
        type: choice
        options:
          - Tama
          - Mike
        # ERROR: Default value is not in options
        default: Chobi
      message:
        type: string
      verbose:
        type: boolean
        # ERROR: Boolean value must be 'true' or 'false'
        default: yes
      age:
        type: number
        # ERROR: Number value must be parsed as a float number
        default: teen

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      # ERROR: Undefined input
      - run: echo "${{ inputs.massage }}"
      # ERROR: Bool value is not available for object key
      - run: echo "${{ env[inputs.verbose] }}"
      # ERROR: Number value is not available for object key
      - run: echo "${{ env[inputs.age] }}"
      # ERROR: `github.event.inputs` is also not defined
      - run: echo "${{ github.event.inputs.massage }}"
```

Output:

```
test.yaml:6:15: input type of workflow_dispatch event must be one of "string", "number", "boolean", "choice", "environment" but got "text" [syntax-check]
  |
6 |         type: text
  |               ^~~~
test.yaml:8:7: input type of "kind" is "choice" but "options" is not set [events]
  |
8 |       kind:
  |       ^~~~~
test.yaml:16:18: default value "Chobi" of "name" input is not included in its options "\"Tama\", \"Mike\"" [events]
   |
16 |         default: Chobi
   |                  ^~~~~
test.yaml:22:18: type of "verbose" input is "boolean". its default value "yes" must be "true" or "false" [events]
   |
22 |         default: yes
   |                  ^~~
test.yaml:26:18: type of "age" input is "number" but its default value "teen" cannot be parsed as a float number: strconv.ParseFloat: parsing "teen": invalid syntax [events]
   |
26 |         default: teen
   |                  ^~~~
test.yaml:33:24: property "massage" is not defined in object type {age: number; id: any; kind: string; message: string; name: string; verbose: bool} [expression]
   |
33 |       - run: echo "${{ inputs.massage }}"
   |                        ^~~~~~~~~~~~~~
test.yaml:35:28: property access of object must be type of string but got "bool" [expression]
   |
35 |       - run: echo "${{ env[inputs.verbose] }}"
   |                            ^~~~~~~~~~~~~~~
test.yaml:37:28: property access of object must be type of string but got "number" [expression]
   |
37 |       - run: echo "${{ env[inputs.age] }}"
   |                            ^~~~~~~~~~~
test.yaml:39:24: property "massage" is not defined in object type {age: string; id: string; kind: string; message: string; name: string; verbose: string} [expression]
   |
39 |       - run: echo "${{ github.event.inputs.massage }}"
   |                        ^~~~~~~~~~~~~~~~~~~~~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNqMkcFOwzAMhu97Cmvi2j5Ar5y5cUMIOa3XmrZ2VTsd07R3R+0CEwpM3KzPn538iUq1Azjq3B8GPb41bBN63a0QgGWKbtcagJuvCsBPE1Xg9OEJ9SxZu+6Ua0pQcKS7AoBOzip2swAKeMYRf4An7m8jDR0wDl7BY6eBEx7JDNvsNPOZpU1woTmoZU5QHQgl338iS/CXzRLHQHM+5ESy271r2DI5mV8n5yhWqFQQQxSPxYBrb2uZ0/T9AsVqVkB1p7B/OJ/Th5QjbgHhctn/ZZIsL8lOUV//qWN7X23ZuxhKWki8zC/0GQAA//+bKqYK)

[`workflow_dispatch`][workflow-dispatch-event] is an event to trigger a workflow manually. The event can have parameters called
'inputs'. Each input has its name, description, default value, and [input type][workflow-dispatch-input-type-announce].

actionlint checks several mistakes around `workflow_dispatch` configuration.

- Input type must be one of 'choice', 'string', 'number', 'boolean', 'environment'
- `options:` must be set for 'choice' input type
- The default value of 'choice' input must be included in options
- The default value of 'boolean' input must be `true` or `false`
- The default value of 'number' input must be parsed as a float number

In addition, `github.event.inputs` and `inputs` objects are typed based on the input definitions. Properties not defined in
`inputs:` will cause a type error thanks to a type checker.

For example,

```yaml
inputs:
  string_input:
    type: string
  choice_input:
    type: choice
    options: ['hello']
  bool_input:
    type: boolean
  num_input:
    type: number
  env_input:
    type: environment
  no_type_input:
```

`inputs` is typed as follows from these definitions:

```
{
  "string_input": string;
  "choice_input": string;
  "bool_input": bool;
  "num_input": number;
  "env_input": string;
  "no_type_input": any;
}
```

`github.event.inputs` is typed as follows since all properties of it are strings unlike `inputs`:

```
{
  "string_input": string;
  "choice_input": string;
  "bool_input": string;
  "num_input": string;
  "env_input": string;
  "no_type_input": string;
}
```

<a id="check-glob-pattern"></a>
## Glob filter pattern syntax validation

Example input:

```yaml
on:
  push:
    branches:
      # ^ is not available for branch name. This kind of mistake is usually caused by misunderstanding
      # that regular expression is available here
      - '^foo-'
    tags:
      # Invalid syntax. + cannot follow special character *
      - 'v*+'
      # Invalid character range 9-1
      - 'v[9-1]'

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - run: echo ...
```

Output:

```
test.yaml:6:10: character '^' is invalid for branch and tag names. ref name cannot contain spaces, ~, ^, :, [, ?, *. see `man git-check-ref-format` for more details. note that regular expression is unavailable. note: filter pattern syntax is explained at https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions#filter-pattern-cheat-sheet [glob]
  |
6 |       - '^foo-'
  |          ^~~~~~
test.yaml:9:12: invalid glob pattern. unexpected character '+' while checking special character + (one or more). the preceding character must not be special character. note: filter pattern syntax is explained at https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions#filter-pattern-cheat-sheet [glob]
  |
9 |       - 'v*+'
  |            ^~
test.yaml:11:14: invalid glob pattern. unexpected character '1' while checking character range in []. start of range '9' (57) is larger than end of range '1' (49). note: filter pattern syntax is explained at https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions#filter-pattern-cheat-sheet [glob]
   |
11 |       - 'v[9-1]'
   |              ^~~
```

[Playground](https://rhysd.github.io/actionlint/#eNpMjMGqAjEMRff9irsrvEcKLu2viEI7VItIMjSJ3y/T2XR1OZzDFc4B2F37sUAdhbfe9CSAEB9PEYqTrbxW8/37jwvdrnS5xxDeUmdlTe2sh7OScIZXZ3P6lMNNpdb25XM4Z7StC1JKvwAAAP//10snvA==)

For filtering branches, tags and paths in Webhook events, [glob syntax][filter-pattern-doc] is available.
actionlint validates glob patterns `branches:`, `branches-ignore:`, `tags:`, `tags-ignore:`, `paths:`, `paths-ignore:` in a
workflow. It checks:

- syntax errors like missing closing brackets for character range `[..]`
- invalid usage like `?` following `*`, invalid character range `[9-1]`, ...
- invalid character usage for Git ref names (branch name, tag name)
  - ref name cannot start/end with `/`
  - ref name cannot contain `[`, `:`, `\`, ...

Most common mistake I have ever seen here is a misunderstanding that regular expression is available for filtering.
This rule can catch the mistake so that users can notice their mistakes.

<a id="check-cron-syntax"></a>
## CRON syntax check at `schedule:`

Example input:

```yaml
on:
  schedule:
    # ERROR: Cron syntax is not correct
    - cron: '0 */3 * *'
    # ERROR: Interval of scheduled job is too small (job runs too frequently)
    - cron: '* */3 * * *'

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - run: echo ...
```

Output:

```
test.yaml:4:13: invalid CRON format "0 */3 * *" in schedule event: expected exactly 5 fields, found 4: [0 */3 * *] [events]
  |
4 |     - cron: '0 */3 * *'
  |             ^~
test.yaml:6:13: scheduled job runs too frequently. it runs once per 60 seconds. the shortest interval is once every 5 minutes [events]
  |
6 |     - cron: '* */3 * * *'
  |             ^~
```

[Playground](https://rhysd.github.io/actionlint/#eNpUjEEKAjEMRfdzir8bCLQK7nobpwYGKYk0yf0lFhezC/+9PJW2AdZPfsXgvIGCPlUa9jvo9gCB9utO/z3J9tbD8tHZfAVmiJU04wjxKOOZ7IfM+WPLyuAMaeB+Kmqt3wAAAP//hKgjxA==)

To trigger a workflow in specific interval, [scheduled event][schedule-event-doc] can be defined in [POSIX CRON syntax][cron-syntax].

actionlint checks the CRON syntax and frequency of running a job. [The official document][schedule-event-doc] says:

> The shortest interval you can run scheduled workflows is once every 5 minutes.

When the job is run more frequently than once every 5 minutes, actionlint reports it as an error.

<a id="check-runner-labels"></a>
## Runner labels

Example input:

```yaml
on: push
jobs:
  test:
    strategy:
      matrix:
        runner:
          # OK
          - macos-latest
          # ERROR: Unknown runner
          - linux-latest
          # OK: Preset labels for self-hosted runner
          - [self-hosted, linux, x64]
          # OK: Single preset label for self-hosted runner
          - arm64
          # ERROR: Unknown label "gpu". Custom label must be defined in actionlint.yaml config file
          - gpu
    runs-on: ${{ matrix.runner }}
    steps:
      - run: echo ...

  test2:
    # ERROR: Too old macOS worker
    runs-on: macos-10.13
    steps:
      - run: echo ...
```

Output:

```
test.yaml:10:13: label "linux-latest" is unknown. available labels are "windows-latest", "windows-latest-8-cores", "windows-2022", "windows-2019", "ubuntu-latest", "ubuntu-latest-4-cores", "ubuntu-latest-8-cores", "ubuntu-latest-16-cores", "ubuntu-24.04", "ubuntu-22.04", "ubuntu-20.04", "macos-latest", "macos-latest-xl", "macos-latest-xlarge", "macos-latest-large", "macos-15-xlarge", "macos-15-large", "macos-15", "macos-14-xl", "macos-14-xlarge", "macos-14-large", "macos-14", "macos-13-xl", "macos-13-xlarge", "macos-13-large", "macos-13", "macos-12-xl", "macos-12-xlarge", "macos-12-large", "macos-12", "self-hosted", "x64", "arm", "arm64", "linux", "macos", "windows". if it is a custom label for self-hosted runner, set list of labels in actionlint.yaml config file [runner-label]
   |
10 |           - linux-latest
   |             ^~~~~~~~~~~~
test.yaml:16:13: label "gpu" is unknown. available labels are "windows-latest", "windows-latest-8-cores", "windows-2022", "windows-2019", "ubuntu-latest", "ubuntu-latest-4-cores", "ubuntu-latest-8-cores", "ubuntu-latest-16-cores", "ubuntu-24.04", "ubuntu-22.04", "ubuntu-20.04", "macos-latest", "macos-latest-xl", "macos-latest-xlarge", "macos-latest-large", "macos-15-xlarge", "macos-15-large", "macos-15", "macos-14-xl", "macos-14-xlarge", "macos-14-large", "macos-14", "macos-13-xl", "macos-13-xlarge", "macos-13-large", "macos-13", "macos-12-xl", "macos-12-xlarge", "macos-12-large", "macos-12", "self-hosted", "x64", "arm", "arm64", "linux", "macos", "windows". if it is a custom label for self-hosted runner, set list of labels in actionlint.yaml config file [runner-label]
   |
16 |           - gpu
   |             ^~~
test.yaml:23:14: label "macos-10.13" is unknown. available labels are "windows-latest", "windows-latest-8-cores", "windows-2022", "windows-2019", "ubuntu-latest", "ubuntu-latest-4-cores", "ubuntu-latest-8-cores", "ubuntu-latest-16-cores", "ubuntu-24.04", "ubuntu-22.04", "ubuntu-20.04", "macos-latest", "macos-latest-xl", "macos-latest-xlarge", "macos-latest-large", "macos-15-xlarge", "macos-15-large", "macos-15", "macos-14-xl", "macos-14-xlarge", "macos-14-large", "macos-14", "macos-13-xl", "macos-13-xlarge", "macos-13-large", "macos-13", "macos-12-xl", "macos-12-xlarge", "macos-12-large", "macos-12", "self-hosted", "x64", "arm", "arm64", "linux", "macos", "windows". if it is a custom label for self-hosted runner, set list of labels in actionlint.yaml config file [runner-label]
   |
23 |     runs-on: macos-10.13
   |              ^~~~~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNqEj8GqwyAQRfd+xV28ZZSXNmThr5QubGqTlESDo5AS8u9FNBSh0JVcZ+ZyjjUSS6CBPe2NJAO8Jh9fgLxTXvevlIBZeTeuRwJcMEa7TwY4ZtVZ4pOKLcVgGk1Yvw0upKcHHyx5fa/SWoW1ba7FlnJz2xQ//RJYpiAeLf62LSOKRIZ9zx56oQOTxwMJ3Q0WQgiWjU+yLEse9b+ozz873gEAAP//SPlVEA==)

GitHub Actions provides two kinds of job runners, [GitHub-hosted runner][gh-hosted-runner] and [self-hosted runner][self-hosted-runner].
Each runner has one or more labels. GitHub Actions runtime finds a proper runner based on label(s) specified at `runs-on:`
to run the job. So specifying proper labels at `runs-on:` is important.

actionlint checks proper label is used at `runs-on:` configuration. Even if an expression is used in the section like
`runs-on: ${{ matrix.foo }}`, actionlint parses the expression and resolves the possible values, then validates the values.

When you define some custom labels for your self-hosted runner, actionlint does not know the labels. Please set the label
names in [`actionlint.yaml` configuration file](config.md) to let actionlint know them.

In addition to checking label values, actionlint checks combinations of labels. `runs-on:` section can be an array that contains
multiple labels. In this case, a runner which has all the labels will be selected. However, those labels combinations can have
conflicts.

Example input:

```yaml
on: push
jobs:
  test:
    runs-on: [ubuntu-latest, windows-latest]
    steps:
      - run: echo ...
```

Output:

```
test.yaml:4:30: label "windows-latest" conflicts with label "ubuntu-latest" defined at line:4,col:15. note: to run your job on each workers, use matrix [runner-label]
  |
4 |     runs-on: [ubuntu-latest, windows-latest]
  |                              ^~~~~~~~~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNosi0EKgDAMBO99xT7A9gH9iniwWqgiSTEJ/b5EPS3LzDBldJMWTi6SA6BV1Be4jSQ6n60YqcVrdThhHLTzkP8vryxau3wdEL3NqFtjpJSeAAAA//9e4h8M)

In most cases, this is a misunderstanding that a matrix combination can be specified at `runs-on:` directly. It should use
`matrix:` and expand it with `${{ }}` at `runs-on:` to run the workflow on multiple runners.

<a id="check-action-format"></a>
## Action format in `uses:`

Example input:

```yaml
on: push
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      # ERROR: ref is missing
      - uses: actions/checkout
      # ERROR: owner name is missing
      - uses: checkout@v2
      # ERROR: tag is empty
      - uses: 'docker://image:'
      # ERROR: local action must start with './'
      - uses: .github/my-actions/do-something
```

Output:

```
test.yaml:7:15: specifying action "actions/checkout" in invalid format because ref is missing. available formats are "{owner}/{repo}@{ref}" or "{owner}/{repo}/{path}@{ref}" [action]
  |
7 |       - uses: actions/checkout
  |               ^~~~~~~~~~~~~~~~
test.yaml:9:15: specifying action "checkout@v2" in invalid format because owner is missing. available formats are "{owner}/{repo}@{ref}" or "{owner}/{repo}/{path}@{ref}" [action]
  |
9 |       - uses: checkout@v2
  |               ^~~~~~~~~~~
test.yaml:11:15: tag of Docker action should not be empty: "docker://image" [action]
   |
11 |       - uses: 'docker://image:'
   |               ^~~~~~~~~~~~~~~~~
test.yaml:13:15: specifying action ".github/my-actions/do-something" in invalid format because ref is missing. available formats are "{owner}/{repo}@{ref}" or "{owner}/{repo}/{path}@{ref}" [action]
   |
13 |       - uses: .github/my-actions/do-something
   |               ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNpczbEOwyAMBNA9X+EtE0XqyNRfAWIBTbGj2K7Uv69olYXppHsnHVOAw6QuT04SFgBF0ZEAp5G44ZaM1NwrDvuRKB7yXwE4MEEJELM2JvG5Yt7ZdOKrfrzvk6wb5x3P4H3rsWBYJ7+VptWS7x93fWzshDtqbVS+AQAA//+oTjwo)

Action needs to be specified in a format defined in [the document][action-uses-doc]. There are 3 types of actions:

- action hosted on GitHub: `owner/repo/path@ref`
- local action: `./path/to/my-action`
- Docker action: `docker://image:tag`

actionlint checks values at `uses:` sections follow one of these formats.

Note that actionlint does not report any error when a directory for a local action does not exist in the repository because it is
a common case where the action is managed in a separate repository and the action directory is cloned at running the workflow.
(See [#25][issue-25] and [#40][issue-40] for more details).

<a id="check-local-action-inputs"></a>
## Local action inputs validation at `with:`

My action definition at `.github/actions/my-action/action.yaml`:

```yaml
name: 'My action'
author: 'rhysd <https://rhysd.github.io>'
description: 'my action'

inputs:
  name:
    description: your name
    default: anonymous
  message:
    description: message to this action
    required: true
  addition:
    description: additional information
    required: false

runs:
  using: 'node20'
  main: 'index.js'
```

Example input:

```yaml
on: push
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      # missing required input "message"
      - uses: ./.github/actions/my-action
      # unexpected input "additions"
      - uses: ./.github/actions/my-action
        with:
          name: rhysd
          message: hello
          additions: foo, bar
```

Output:
<!-- Skip update output -->

```
test.yaml:7:15: missing input "message" which is required by action "My action" defined at "./.github/actions/my-action". all required inputs are "message" [action]
  |
7 |       - uses: ./.github/actions/my-action
  |               ^~~~~~~~~~~~~~~~~~~~~~~~~~~
test.yaml:13:11: input "additions" is not defined in action "My action" defined at "./.github/actions/my-action". available inputs are "addition", "message", "name" [action]
   |
13 |           additions: foo, bar
   |           ^~~~~~~~~~
```

<!-- Skip playground link -->

When a local action is run in `uses:` of `step:`, actionlint reads `action.yml` file in the local action directory and
validates inputs at `with:` in the workflow are correct. Missing required inputs and unexpected inputs can be detected.

<a id="check-popular-action-inputs"></a>
## Popular action inputs validation at `with:`

Example input:

```yaml
on: push

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/cache@v4
        with:
          keys: |
            ${{ hashFiles('**/*.lock') }}
            ${{ hashFiles('**/*.cache') }}
          path: ./packages
      - run: make
```

Output:

```
test.yaml:7:15: missing input "key" which is required by action "actions/cache@v4". all required inputs are "key", "path" [action]
  |
7 |       - uses: actions/cache@v4
  |               ^~~~~~~~~~~~~~~~
test.yaml:9:11: input "keys" is not defined in action "actions/cache@v4". available inputs are "enableCrossOsArchive", "fail-on-cache-miss", "key", "lookup-only", "path", "restore-keys", "save-always", "upload-chunk-size" [action]
  |
9 |           keys: |
  |           ^~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNqEjrHKwkAQhPs8xRQ/5DeQpLG6ysr32ByLFy/eHdlbRWLeXRIliI3VsvN9MBODQVJxRXGOnZgCyCx5ucCoQepF0E5D1nqgha1IMid5WUANFRYDsrmPQVpL1vHhun9j4NZnZ7YP8HwXg8dHAvxNExyJO/YDy39ZVW3VDNH6cod5/mmuld9qouwMmjaR9XRi2eaOGgwu5PkZAAD///tlRBU=)

actionlint checks inputs of many popular actions such as `actions/checkout@v4`. It checks

- some input is required by the action but it is not set at `with:`
- input set at `with:` is not defined in the action (this commonly occurs by a typo)

this is done by checking `with:` section items with a small database collected at building `actionlint` binary. actionlint
can check popular actions without fetching any `action.yml` of the actions from the remote so that it can run efficiently.

Note that it only supports the case of specifying major versions like `actions/checkout@v4`. Fixing version of action like
`actions/checkout@v4.0.1` and using the HEAD of action like `actions/checkout@main` are not supported for now.

So far, actionlint supports more than 100 popular actions The data set is embedded at [`popular_actions.go`](../popular_actions.go)
and were automatically collected by [a script][generate-popular-actions]. If you want more checks for other actions, please
make a request [as an issue][issue-form].

<a id="detect-outdated-popular-actions"></a>
## Outdated popular actions detection at `uses:`

Example input:

```yaml
on: push

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      # ERROR: actions/checkout@v3 is using the outdated runner 'node16'
      - uses: actions/checkout@v3
```

Output:

```
test.yaml:8:15: the runner of "actions/checkout@v3" action is too old to run on GitHub Actions. update the action's version to fix this issue [action]
  |
8 |       - uses: actions/checkout@v3
  |               ^~~~~~~~~~~~~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNokyjEOxCAMRNGeU8wF0BbbUe1VAFlik8hGGTvnj0iqX/xnWjCDI6XNGksCXOirwBnKvEC0UI981PWeRZfJVwEZQWFB7f435acP6buF/67vHQAA//90iR3O)

In addition to the checks for inputs of actions described in [the previous section](#check-popular-action-inputs), actionlint
reports an error when a popular action is 'outdated'. An action is outdated when the runner used by the action is no longer
supported by GitHub Actions runtime. For example, `node12` is no longer available so any actions can not use `node12` runner.

Note that this check doesn't report that the action version is up-to-date. For example, even if you use `actions/checkout@v4` and
newer version `actions/checkout@v5` is available, actionlint reports no error as long as `actions/checkout@v4` is not outdated.
If you want to keep actions used by your workflows up-to-date, consider to use [Dependabot][dependabot-doc].

<a id="check-shell-names"></a>
## Shell name validation at `shell:`

Example input:

```yaml
on: push
jobs:
  linux:
    runs-on: ubuntu-latest
    steps:
      - run: echo 'hello'
        # ERROR: Unavailable shell
        shell: dash
      - run: echo 'hello'
        # ERROR: 'powershell' is only available on Windows
        shell: powershell
  mac:
    runs-on: macos-latest
    defaults:
      run:
        # ERROR: default config is also checked. fish is not supported
        shell: fish
    steps:
      - run: echo 'hello'
        # OK: Custom shell
        shell: 'perl {0}'
  windows:
    runs-on: windows-latest
    steps:
      - run: echo 'hello'
        # ERROR: 'sh' is only available on Windows
        shell: sh
      - run: echo 'hello'
        # OK: 'powershell' is only available on Windows
        shell: powershell
```

Output:

```
test.yaml:8:16: shell name "dash" is invalid. available names are "bash", "pwsh", "python", "sh" [shell-name]
  |
8 |         shell: dash
  |                ^~~~
test.yaml:11:16: shell name "powershell" is invalid on macOS or Linux. available names are "bash", "pwsh", "python", "sh" [shell-name]
   |
11 |         shell: powershell
   |                ^~~~~~~~~~
test.yaml:17:16: shell name "fish" is invalid. available names are "bash", "pwsh", "python", "sh" [shell-name]
   |
17 |         shell: fish
   |                ^~~~
test.yaml:27:16: shell name "sh" is invalid on Windows. available names are "bash", "cmd", "powershell", "pwsh", "python" [shell-name]
   |
27 |         shell: sh
   |                ^~
```

[Playground](https://rhysd.github.io/actionlint/#eNqkkM2qgzAQhfc+xdm5Eu46bxN1JF7GTHAyWCh99zJWSnHVn91JzkfycSQHFNPU/EuvoQF4znbxAKyWtXPAesvVOo6VtO6VVir6oIDOyQAakqBNxCzt0QDq54AxanqfLrLRuucGWOJw0lniIPpqM9IUjetTyD84vznNh8Gn6m2hlXH9u3mzzXmUTU9Cx+0vA303zz0AAP//Pn9+pA==)

Available shells for runners are defined in [the documentation][shell-doc]. actionlint checks shell names at `shell:`
configuration are properly using the available shells.

<a id="check-job-step-ids"></a>
## Job ID and step ID uniqueness

Example input:

```yaml
on: push
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - run: echo 'hello'
        id: step_id
      - run: echo 'bye'
        # ERROR: Duplicate step ID
        id: STEP_ID
  # ERROR: Duplicate job ID
  TEST:
    runs-on: ubuntu-latest
    steps:
      - run: echo 'hello'
        # OK. Step ID uniqueness is job-local
        id: step_id
```

Output:

```
test.yaml:10:13: step ID "STEP_ID" duplicates. previously defined at line:7,col:13. step ID must be unique within a job. note that step ID is case insensitive [id]
   |
10 |         id: STEP_ID
   |             ^~~~~~~
test.yaml:12:3: key "TEST" is duplicated in "jobs" section. previously defined at line:3,col:3. note that this key is case insensitive [syntax-check]
   |
12 |   TEST:
   |   ^~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNrKz7NSKCgtzuDKyk8qtuJSUChJLS4B0QoKRaV5xbog+dKk0rySUt2cRJAcWKq4JLWgGKJKQUEXpNJKITU5I19BPSM1JydfHSqjoJCZYgVWHJ+Zgk11UmUqqtrgENeAeE8XLgWFENfgEJq4AxAAAP//ioFDag==)

Job IDs and step IDs in each jobs must be unique. IDs are compared in case-insensitive. actionlint checks all job IDs
and step IDs, and reports errors when some IDs duplicate.

<a id="check-hardcoded-credentials"></a>
## Hardcoded credentials

Example input:

```yaml
on: push
jobs:
  test:
    runs-on: ubuntu-latest
    container:
      image: 'example.com/owner/image'
      credentials:
        username: user
        # ERROR: Hardcoded password
        password: pass
    services:
      redis:
        image: redis
        credentials:
          username: user
          # ERROR: Hardcoded password
          password: pass
    steps:
      - run: echo 'hello'
```

Output:

```
test.yaml:10:19: "password" section in "container" section should be specified via secrets. do not put password value directly [credentials]
   |
10 |         password: pass
   |                   ^~~~
test.yaml:17:21: "password" section in "redis" service should be specified via secrets. do not put password value directly [credentials]
   |
17 |           password: pass
   |                     ^~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNp0kLFuxSAMRff3Fd6Y0rfzNzxy1VCBjWxo+vkVNEUd0gnrHF8uQthT7XY8PuRl/kHUYG2cRNrZtuH7q3PrWw7DTRWFW0gM/dkkSiW8w5PDVyg14y1KecrJ0Oc07lqLih3cUsj2myTqBuVQ4Oe0cA1mp+ju5zSxQT9TxMoq9vTnousRky54W/lv6X1tQ13hbfyLJ8RDyB3IWdx3AAAA//9SS13D)

[Credentials for container][credentials-doc] can be put in `container:` configuration. Password should be put in secrets
and the value should be expanded with `${{ }}` syntax at `password:`. actionlint checks hardcoded credentials, and reports
them as an error.

<a id="check-env-var-names"></a>
## Environment variable names

Example input:

```yaml
on: push
jobs:
  test:
    runs-on: ubuntu-latest
    env:
      FOO=BAR: foo
      FOO BAR: foo
    steps:
      - run: echo 'hello'
```

Output:

```
test.yaml:6:7: environment variable name "FOO=BAR" is invalid. '&', '=' and spaces should not be contained [env-var]
  |
6 |       FOO=BAR: foo
  |       ^~~~~~~~
test.yaml:7:7: environment variable name "FOO BAR" is invalid. '&', '=' and spaces should not be contained [env-var]
  |
7 |       FOO BAR: foo
  |       ^~~
```

[Playground](https://rhysd.github.io/actionlint/#eNrKz7NSKCgtzuDKyk8qtuJSUChJLS4B0QoKRaV5xbog+dKk0rySUt2cRJAcWCo1rwyiRkHBzd/f1skxyEohLT8fIaSAIlRcklpQDNOgCzLYSiE1OSNfQT0jNScnXx0QAAD///hYJMc=)

`=` must not be included in environment variable names. And `&` and spaces should not be included in them. In almost all
cases they are mistakes, and they may cause some issues on using them in shell since they have special meaning in shell syntax.

actionlint checks environment variable names are correct in `env:` configuration.

<a id="permissions"></a>
## Permissions

Example input:

```yaml
on: push

# ERROR: Available values for whole permissions are "write-all", "read-all" or "none"
permissions: write

jobs:
  test:
    runs-on: ubuntu-latest
    permissions:
      # ERROR: "checks" is correct scope name
      check: write
      # ERROR: Available values are "read", "write" or "none"
      issues: readable
    steps:
      - run: echo hello
```

Output:

```
test.yaml:4:14: "write" is invalid for permission for all the scopes. available values are "read-all" and "write-all" [permissions]
  |
4 | permissions: write
  |              ^~~~~
test.yaml:11:7: unknown permission scope "check". all available permission scopes are "actions", "attestations", "checks", "contents", "deployments", "discussions", "id-token", "issues", "packages", "pages", "pull-requests", "repository-projects", "security-events", "statuses" [permissions]
   |
11 |       check: write
   |       ^~~~~~
test.yaml:13:15: "readable" is invalid for permission of scope "issues". available values are "read", "write" or "none" [permissions]
   |
13 |       issues: readable
   |               ^~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNpMjVGqQyEMRP9dxWzADbgb9QX01RpxErr94r209CtwhnOiM2E5WwhL9rOTXScTXrubhPCvhSkAJrRzge2T8UhefJrHkc92Tb+BCwC1SX18ajfqpAsTtuS/XMaNabK+UjxPEqQ2RZMx9B0AAP//57Yy6Q==)

Permissions of `GITHUB_TOKEN` token can be configured at workflow-level or job-level by [`permissions:` section][perm-config-doc].
Each permission scopes have its access levels. The default levels are described in [the document][permissions-doc].

actionlint checks permission scopes and access levels in a workflow are correct.

<a id="check-reusable-workflows"></a>
## Reusable workflows

[Reusable workflows][reusable-workflow-doc] is a feature to call a workflow from another workflow.

actionlint does several checks for both workflow calls (caller) and reusable workflows (callee):

- syntax of workflow calls and reusable workflows
- type checks for inputs (respecting `type:` field of each input) in both workflow calls and reusable workflows
- type checks for `inputs`, `outputs` and `secrets` context objects in reusable workflows
- optional/required/undefined inputs and secrets at `uses:` in workflow calls
- type checks for `outputs` objects used by downstream jobs of workflow calls

These checks are described in this section.

### Check input definitions of `workflow_call` event in reusable workflow

Example input:

```yaml
on:
  workflow_call:
    inputs:
      scheme:
        description: Scheme of URL
        # OK: Type is string
        default: https
        type: string
      host:
        default: example.com
        type: string
      port:
        description: Port of URL
        # ERROR: Type is number but default value is string
        default: ':1234'
        type: number
      query:
        description: Query of URL
        # ERROR: Type must be one of number, string, boolean
        type: object
      path:
        description: Path of URL
        required: true
        # ERROR: Default value is never used since this input is required
        default: ''
        type: string
jobs:
  do:
    runs-on: ubuntu-latest
    steps:
      - run: echo "${{ inputs.scheme }}://${{ inputs.host }}:${{ inputs.port }}${{ inputs.path }}"
```

Output:

```
test.yaml:15:18: input of workflow_call event "port" is typed as number but its default value ":1234" cannot be parsed as a float number: strconv.ParseFloat: parsing ":1234": invalid syntax [events]
   |
15 |         default: ':1234'
   |                  ^~~~~~~
test.yaml:20:15: invalid value "object" for input type of workflow_call event. it must be one of "boolean", "number", or "string" [syntax-check]
   |
20 |         type: object
   |               ^~~~~~
test.yaml:25:18: input "path" of workflow_call event has the default value "", but it is also required. if an input is marked as required, its default value will never be used [events]
   |
25 |         default: ''
   |                  ^~
```

[Playground](https://rhysd.github.io/actionlint/#eNp8kbtu8zAMhff/KYjgBzI5QS+TnqFDL+hcyDJdOZVFhaKQBoHevVDsBIYbd7M/kodHPOTVP4AD8Vfr6PBhtHMFAHQ+JInDN0A0Fnu8/AE0GA13QTryCt7ORaAW3l+fJi2tTk4UWJEQr1iOARVE4c5/jtBSFPV7Dr91HxxuDPV/TQdiWTD2TCyLttbq7v7hcT2T9qmvkUe4T8jHBe2XUpuLDxpU79DIxZ4Wu2RPi50rMO5Tx9goEE54w/X69i12VJ/DamhYxsnHqixJdfKSKqcF4+ApCoZrsFXpVIDGEqz+n05j7pshcMhZbbcTXKIqcILK/SHnKSnvynn1EwAA//+1Oa7A)

Unlike inputs of action, inputs of a workflow must specify their types. actionlint validates input types and checks the default
values are correctly typed. For more details, see [the official document][create-reusable-workflow-doc].

### Check workflow call syntax

Example input:

```yaml
on: push
jobs:
  job1:
    uses: owner/repo/path/to/workflow.yml@v1
    # ERROR: 'runs-on' is not available on calling reusable workflow
    runs-on: ubuntu-latest
  job2:
    # ERROR: Local file path with ref is not available
    uses: ./.github/workflows/ci.yml@main
  job3:
    # ERROR: 'with' is only available on calling reusable workflow
    with:
      foo: bar
    runs-on: ubuntu-latest
    steps:
      - run: echo hello
  job4:
    # ERROR: This workflow does not exist
    uses: ./.github/workflows/not-existing.yml
```

Output:

```
test.yaml:6:5: when a reusable workflow is called with "uses", "runs-on" is not available. only following keys are allowed: "name", "uses", "with", "secrets", "needs", "if", and "permissions" in job "job1" [syntax-check]
  |
6 |     runs-on: ubuntu-latest
  |     ^~~~~~~~
test.yaml:9:11: reusable workflow call "./.github/workflows/ci.yml@main" at "uses" is not following the format "owner/repo/path/to/workflow.yml@ref" nor "./path/to/workflow.yml". see https://docs.github.com/en/actions/learn-github-actions/reusing-workflows for more details [workflow-call]
  |
9 |     uses: ./.github/workflows/ci.yml@main
  |           ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
test.yaml:12:5: "with" is only available for a reusable workflow call with "uses" but "uses" is not found in job "job3" [syntax-check]
   |
12 |     with:
   |     ^~~~~
test.yaml:19:11: could not read reusable workflow file for "./.github/workflows/not-existing.yml": open /path/to/repo/.github/workflows/not-existing.yml: no such file or directory [workflow-call]
   |
19 |     uses: ./.github/workflows/not-existing.yml
   |           ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNqEzjFywyAQBdBep9gLIEZJKqpcBTQrgYJYhl1CcnsPYuxxZVdQ/P/2UzKQK/vpIMdmAjjILf0FqIxsgFrCogtm0tmK10K6UfnZIrX5/4zfv8sVLjWx6lh1NUlV0QqyDO/j2Zv1vAfx1T0Y1mu4qNOGNBqfo9GC+PED2IgMOFteXwNgwcz3kupJA7h6Ao8x0uC/3g1KJAr/AktIe592CwAA//9tJVzI)

When calling an external workflow, [only specific keys are available][reusable-workflow-call-keys] at job configuration.
For example, `secrets:` is not available when running steps in a normal job. And `runs-on:` is not available when calling
a reusable workflow since the called workflow determines which OS is used. actionlint checks such keys are used correctly
to call a reusable workflow or to run steps in a normal job.

And the workflow syntax at `uses:` must follow the format `owner/repo/path/to/workflow.yml@ref` as described in
[the official document][create-reusable-workflow-doc]. actionlint checks if the value follows the format.

actionlint also validates the called workflow file is actually existing when it is a local workflow (starting with `./`).
actionlint reports an error when it does not exist.

### Check types of `inputs.*` and `secrets.*` in reusable workflow

Example input:

```yaml
on:
  workflow_call:
    inputs:
      url:
        description: 'your URL'
        type: string
      lucky_number:
        description: 'your lucky number'
        type: number
    secrets:
      credential:
        description: 'your credential'

jobs:
  test:
    runs-on: ubuntu-20.04
    steps:
      - name: Send data
        # ERROR: uri is typo of url
        run: curl ${{ inputs.uri }} -d ${{ inputs.lucky_number }}
        env:
          # ERROR: credentials is typo of credential
          TOKEN: ${{ secrets.credentials }}
```

Output:

```
test.yaml:20:23: property "uri" is not defined in object type {lucky_number: number; url: string} [expression]
   |
20 |         run: curl ${{ inputs.uri }} -d ${{ inputs.lucky_number }}
   |                       ^~~~~~~~~~
test.yaml:23:22: property "credentials" is not defined in object type {actions_runner_debug: string; actions_step_debug: string; credential: string; github_token: string} [expression]
   |
23 |           TOKEN: ${{ secrets.credentials }}
   |                      ^~~~~~~~~~~~~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNp8UL1q80AQ7PUUU3ygSsJ8pLo+VUIC+amNdNqEi897Yu82Rph792CdIpkU7pbZ+dnZwKYCTkEOHz6c9rbz/gIAjkdNscyAiv8dgYGiFTcmF9ignoIK3l8e63WfppEMYhLHnwvo1R6mPeuxJ7lpNBNRiH8dCzqDkazQdp4VGoiT625fudHqqvoK/axPFFNRiXJsLmztlZM2/3ft7q7EJRrXsAbcHcnglXjA0KVujRRlA6vi8e98Xj7YqjjkjGa4Bq//gZxXB+LvrQHw9vxw/2Rm4dK43TpE5PwTAAD//6lWhKI=)

Inputs of reusable workflow calls are set to `inputs.*` properties following the definitions at `on.workflow_call.inputs`.
And in a job of a reusable workflow, `secrets.*` are passed from caller of the workflow so it is set following the definitions at
`on.workflow_call.secrets`. See [the official document][create-reusable-workflow-doc] for more details.

actionlint contextually defines types of `inputs` and `secrets` contexts looking at `workflow_call` event. Keys of `inputs` only
allow keys at `on.workflow_call.inputs` and their values are typed based on `on.workflow_call.inputs.<input_name>.type`. Type of
`secrets` is also strictly typed following `on.workflow_call.secrets`.

[From May 3, 2022][inherit-secrets-announce], GitHub Actions allows inheriting secrets by calling reusable workflows. The caller
declares to inherit all secrets.

```yaml
jobs:
  pass-secrets-to-workflow:
    uses: ./.github/workflows/called-workflow.yml
    secrets: inherit
```

This means that actionlint cannot know whether the workflow inherits secrets or not when checking a reusable workflow.
To solve this issue, actionlint assumes that

- when `secrets:` is omitted in a reusable workflow, the workflow inherits secrets from a caller
- when `secrets:` exists in a reusable workflow, the workflow inherits no other secret

Following the assumptions,

```yaml
on:
  workflow_call:

jobs:
  pass-secret-to-action:
    runs-on: ubuntu-latest
    steps:
      # OK: This reports no error. FOO is assumed to be inherited from caller
      - run: echo ${{ secrets.FOO }}
```

this workflow causes no error. And

```yaml
on:
  workflow_call:
    secrets:

jobs:
  pass-secret-to-action:
    runs-on: ubuntu-latest
    steps:
      # ERROR: Secret FOO is not defined
      - run: echo ${{ secrets.FOO }}
```

this workflow causes 'no such secret' error at `secrets.FOO`.

### Check outputs in reusable workflow

Example input:

```yaml
on:
  workflow_call:
    outputs:
      image-version:
        description: "Docker image version"
        # ERROR: 'imagetag' does not exist (typo of 'image_tag')
        value: ${{ jobs.gen-image-version.outputs.imagetag }}
jobs:
  gen-image-version:
    runs-on: ubuntu-latest
    outputs:
      image_tag: "${{ steps.get_tag.outputs.tag }}"
    steps:
      - run: ./output_image_tag.sh
        id: get_tag
```

Output:

```
test.yaml:7:20: property "imagetag" is not defined in object type {image_tag: string} [expression]
  |
7 |         value: ${{ jobs.gen-image-version.outputs.imagetag }}
  |                    ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNp0j7FuxSAMRff3FVbUFboz9z8iXuJSGgoRNskQ5d8rxylSVZWNq2Of65LdA2AvdXlPZR8nn5IEAKXx2pj0AxC/fECzYaWoI/pmpKnGlSWE4a1MC1Zl4WaHzm4+NXTwchzwWZ5kA2bza629nfZK2Qc4z4eg4vtDa4naMhmRt2fL3EzyjMT/XjCyDw4G6UCMq5RgybparVr6In7Gjagc2Fclx77O0kc/Mc4O7o3fAQAA///Y5G5j)

Outputs of a reusable workflow can be defined at `on.workflow_call.outputs` as described in [the document][reusable-workflow-outputs].
The `jobs` context is available to define an output value to refer the outputs of jobs in the workflow. actionlint checks
the context is used correctly.

### Check inputs and secrets in workflow call

Example reusable workflow:

```yaml
# .github/workflows/reusable.yaml
on:
  workflow_call:
    inputs:
      name:
        type: string
        required: true
      id:
        type: number
      message:
        type: string
    secrets:
      password:
        required: true

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - run: echo '${{ outputs.required_input }}'
```

Example input:

```yaml
on: push

jobs:
  # Check required/undefined inputs and secrets
  missing-required:
    uses: ./.github/workflows/reusable.yaml
    with:
      # ERROR: Undefined input
      user: rhysd
      # ERROR: Required input "name" is missing
    secrets:
      # ERROR: Undefined secret
      credentials: my-token
      # ERROR: Required secret "password" is missing

  # Check types of inputs defined in reusable workflow
  type-checks:
    uses: ./.github/workflows/reusable.yaml
    with:
      name: rhysd
      # ERROR: Cannot assign bool value to number input
      id: true
      # ERROR: Cannot assign null to string input. If you want to pass string "null", use ${{ 'null' }}
      message: null
    secrets:
      password: p@ssw0rd
```

Output:
<!-- Skip update output -->

```
test.yaml:6:11: input "name" is required by "./.github/workflows/reusable.yaml" reusable workflow [workflow-call]
  |
6 |     uses: ./.github/workflows/reusable.yaml
  |           ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
test.yaml:6:11: secret "password" is required by "./.github/workflows/reusable.yaml" reusable workflow [workflow-call]
  |
6 |     uses: ./.github/workflows/reusable.yaml
  |           ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
test.yaml:9:7: input "user" is not defined in "./.github/workflows/reusable.yaml" reusable workflow. defined inputs are "id", "message", "name" [workflow-call]
  |
9 |       user: rhysd
  |       ^~~~~
test.yaml:13:7: secret "credentials" is not defined in "./.github/workflows/reusable.yaml" reusable workflow. defined secret is "password" [workflow-call]
   |
13 |       credentials: my-token
   |       ^~~~~~~~~~~~
test.yaml:22:11: input "id" is typed as number by reusable workflow "./.github/workflows/reusable.yaml". bool value cannot be assigned [expression]
   |
22 |       id: true
   |           ^~~~
test.yaml:24:16: input "message" is typed as string by reusable workflow "./.github/workflows/reusable.yaml". null value cannot be assigned [expression]
   |
24 |       message: null
   |                ^~~~
```

<!-- Skip playground link -->

Reusable workflows can define required/optional inputs and secrets. When they are missing or some undefined input is used in a
workflow call, actionlint reports an error.

And reusable workflows must define types of their inputs by `type:` field. Workflow calls pass constants (`input: 42`) or
expressions (`inputs: ${{ ... }}`) to the inputs or secrets. actionlint checks types of values passed to inputs in workflow call.
When a type of input doesn't match to its definition, actionlint reports an error.

Note that this check only works with local reusable workflow (it starts with `./`).

### Check outputs of workflow call in downstream jobs

Example reusable workflow:

```yaml
# .github/workflows/get-build-info.yaml
on:
  workflow_call:
    outputs:
      version:
        value: ${{ outputs.version }}
        description: version of software

jobs:
  test:
    runs-on: ubuntu-latest
    outputs:
      version: ${{ steps.get_version.outputs.version }}
    steps:
      - run: ...
        id: get_version
```

Example input:

```yaml
on: push

jobs:
  get_build_info:
    uses: ./.github/workflows/get-build-info.yaml
  downstream:
    needs: [get_build_info]
    runs-on: ubuntu-latest
    steps:
      # OK. `version` is defined in the reusable workflow
      - run: echo '${{ needs.get_build_info.outputs.version }}'
      # ERROR: `tag` is not defined in the reusable workflow
      - run: echo '${{ needs.get_build_info.outputs.tag }}'
```

Output:
<!-- Skip update output -->

```
test.yaml:13:24: property "tag" is not defined in object type {version: string} [expression]
   |
13 |       - run: echo '${{ needs.get_build_info.outputs.tag }}'
   |                        ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
```

<!-- Skip playground link -->

Outputs of workflow call are set to the job's outputs object. They can be accessed by downstream jobs specified with `needs:`.
What outputs are set is defined in the reusable workflow. actionlint types outputs objects from workflow calls and check the
object types in downstream jobs.

In the above example, `get-build-info.yaml` has one output `version`. actionlint types the outputs object of workflow call job
as `{version: string}`. In the downstream job, actionlint can report an error at undefined key `tag` in the object.

Note that this check only works with local reusable workflow (starting with `./`).

<a id="id-naming-convention"></a>
## ID naming convention

Example input:

```yaml
on: push

jobs:
  # ERROR: '.' cannot be contained in ID
  foo-v1.2.3:
    runs-on: ubuntu-latest
    steps:
      - run: echo 'job ID with version'
        # ERROR: ID cannot contain spaces
        id: echo for test
  # ERROR: ID cannot start with '-'
  -hello-world-:
    runs-on: ubuntu-latest
    steps:
      - run: echo 'oops'
  # ERROR: ID cannot start with numbers
  2d-game:
    runs-on: ubuntu-latest
    steps:
      - run: echo 'oops'
```

Output:

```
test.yaml:5:3: invalid job ID "foo-v1.2.3". job ID must start with a letter or _ and contain only alphanumeric characters, -, or _ [id]
  |
5 |   foo-v1.2.3:
  |   ^~~~~~~~~~~
test.yaml:10:13: invalid step ID "echo for test". step ID must start with a letter or _ and contain only alphanumeric characters, -, or _ [id]
   |
10 |         id: echo for test
   |             ^~~~
test.yaml:12:3: invalid job ID "-hello-world-". job ID must start with a letter or _ and contain only alphanumeric characters, -, or _ [id]
   |
12 |   -hello-world-:
   |   ^~~~~~~~~~~~~~
test.yaml:17:3: invalid job ID "2d-game". job ID must start with a letter or _ and contain only alphanumeric characters, -, or _ [id]
   |
17 |   2d-game:
   |   ^~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNqkzTEOgzAMheGdU7yNyUilG3OXHgOKaUBpHooTuH6VNjdgtPzp/QwD9myuaTZONjTAQspx6/ruXi4g5mBSWJ5ySFn8mNTS72VJd/srQIocoC9HtBsnPB841+RwaLSVoa0OWOfKFkbUMXHqPeVk9LNcCJO7lVI/y3v86NWlbwAAAP//gyJUhw==)

IDs must start with a letter or `_` and contain only alphanumeric characters, `-` or `_`. actionlint checks the naming
convention, and reports invalid IDs as errors.

<a id="ctx-spfunc-availability"></a>
## Contexts and special functions availability

Example input:

```yaml
on: push

env:
  NAME: rhysd

jobs:
  test:
    strategy:
      matrix:
        directory:
          # OK: 'github' context is available here
          - ${{ github.workflow }}
          # ERROR: 'runner' context is not available here
          - ${{ runner.temp }}
    runs-on: ubuntu-latest
    env:
      # ERROR: 'env' context is not available here
      NAME: ${{ env.NAME }}
    steps:
      - env:
          # OK: 'env' context is available here
          NAME: ${{ env.NAME }}
        # ERROR: 'success()' function is not available here
        run: echo 'Success? ${{ success() }}'
        # OK: 'success()' function is available here
        if: success()
```

Output:

```
test.yaml:14:17: context "runner" is not allowed here. available contexts are "github", "inputs", "needs", "vars". see https://docs.github.com/en/actions/learn-github-actions/contexts#context-availability for more details [expression]
   |
14 |           - ${{ runner.temp }}
   |                 ^~~~~~~~~~~
test.yaml:18:17: context "env" is not allowed here. available contexts are "github", "inputs", "matrix", "needs", "secrets", "strategy", "vars". see https://docs.github.com/en/actions/learn-github-actions/contexts#context-availability for more details [expression]
   |
18 |       NAME: ${{ env.NAME }}
   |                 ^~~~~~~~
test.yaml:24:33: calling function "success" is not allowed here. "success" is only available in "jobs.<job_id>.if", "jobs.<job_id>.steps.if". see https://docs.github.com/en/actions/learn-github-actions/contexts#context-availability for more details [expression]
   |
24 |         run: echo 'Success? ${{ success() }}'
   |                                 ^~~~~~~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNp8jj1OwzAYhnef4h2QCkNyAC+IgREWTpCkXxtDa0ffT0tU+e7ITU2QkOrFev/sJ0WPyWR0juLJO+D95e3Vg8dZts59pl6KqSRabkCUO6X9vCjg2CmH76qAbWAaNPG8WkCDh8sF+6Cj9e058dfukM7I+V+FLUbiVuk41ZgtSlMorbeo1hy6AnONbsTlLNTlCYqntqi6F6VJaq35u7m/u/3tQcOYsPmwYSCR52tXFvH4hJw3v+2w82vyEwAA//9WsV7P)

Some contexts are only available in some places. For example, `env` context is not available at `jobs.<job_id>.env`, but it is
available at `jobs.<job_id>.steps.env`.

Similarly, some status functions are special since they limit where they can be called. For example, `success()`, `failure()`,
`always()`, and `cancelled()` are only available at `if:` section. At the time of writing this document, the following functions
are special.

- `hashFiles()`
- `always()`
- `success()`
- `failure()`
- `cancelled()`

[The official contexts document][availability-doc] describes which contexts and special functions are available at which workflow
keys.

actionlint checks if these contexts and special functions are used correctly. It reports an error when it finds that some context
or special function is not available in your workflow.

<a id="#check-deprecated-workflow-commands"></a>
## Check deprecated workflow commands

Example input:

```yaml
on: push

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      # ERROR: 'set-output' workflow command was deprecated
      - run: echo '::set-output name=foo::bar'
      # OK: Use this instead
      - run: echo "foo=bar" >> "$GITHUB_OUTPUT"
      # OK: 'debug' command is not deprecated
      - run: echo "::debug::Set the Octocat variable"
```

Output:

```
test.yaml:8:14: workflow command "set-output" was deprecated. use `echo "{name}={value}" >> $GITHUB_OUTPUT` instead: https://docs.github.com/en/actions/using-workflows/workflow-commands-for-github-actions [deprecated-commands]
  |
8 |       - run: echo '::set-output name=foo::bar'
  |              ^~~~
```

[Playground](https://rhysd.github.io/actionlint/#eNpsyjGOgzAQRuGeU/yyVqLyBUaCYpvdrVgpUEc2GUIi4kH2TM4fOWmpXvE+SYTdyto0d4mFGkC5aC2QLRVfgUVLan4L9b1XUd7LRwG+SgLPq6AlKqxeTHdTpPDgbhEhiiG3B9wtIl0M2aHv4b5+/sbf6fs8TOP/NLojT3ThaFeiEyt0ZQyzyhwUz5BvIW7sXgEAAP//zI0+6A==)

GitHub deprecated the following workflow commands.

- [`set-output`][deprecate-set-output-save-state]
- [`save-state`][deprecate-set-output-save-state]
- [`set-env`][deprecate-set-env-add-path]
- [`add-path`][deprecate-set-env-add-path]

actionlint detects these commands are used in `run:` and reports them as errors suggesting alternatives. See
[the official document][workflow-commands-doc] for the comprehensive list of workflow commands to know the usage.

<a id="if-cond-always-true"></a>
## Conditions always evaluated to true at `if:`

Example input:

```yaml
on: push

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - run: echo 'Commit is pushed'
        # OK
        if: ${{ github.event_name == 'push' }}
      - run: echo 'Commit is pushed'
        # OK
        if: |
          github.event_name == 'push'
      - run: echo 'Commit is pushed'
        # ERROR: It is always evaluated to true
        if: |
          ${{ github.event_name == 'push' }}
      - run: echo 'Commit is pushed'
        # ERROR: It is always evaluated to true
        if: "${{ github.event_name == 'push' }} "
      - run: echo 'Commit is pushed to main'
        # OK
        if: github.event_name == 'push' && github.ref_name == 'main'
      - run: echo 'Commit is pushed to main'
        # ERROR: It is always evaluated to true
        if: ${{ github.event_name == 'push' }} && ${{ github.ref_name == 'main' }}
```

Output:

```
test.yaml:16:13: if: condition "${{ github.event_name == 'push' }}\n" is always evaluated to true because extra characters are around ${{ }} [if-cond]
   |
16 |         if: |
   |             ^
test.yaml:20:13: if: condition "${{ github.event_name == 'push' }} " is always evaluated to true because extra characters are around ${{ }} [if-cond]
   |
20 |         if: "${{ github.event_name == 'push' }} "
   |             ^~~~
test.yaml:26:13: if: condition "${{ github.event_name == 'push' }} && ${{ github.ref_name == 'main' }}" is always evaluated to true because extra characters are around ${{ }} [if-cond]
   |
26 |         if: ${{ github.event_name == 'push' }} && ${{ github.ref_name == 'main' }}
   |             ^~~
```

[Playground](https://rhysd.github.io/actionlint/#eNq0j02qwjAURuddxUd5JKO+BQQ6egt5tHprIyYpzY2Tmr1L6l9FqFVxFMI5nC9xVqELvs2yrau9ygAmz+kE+mB9kYRQB8uh2FWJjcgzdf5kAUUyFWjVOsg/Z4xmaD9maS3PEqAbhZ9hwEZzG+pf2pPlf1sZQllCJlsixtebh+sNc+3Pwt94eP48inxJFuxgKm3v83NlIS64p+YGp5E3Fhf8R4ip9TiOGI8BAAD//6IAwso=)

Evaluation of `${{ }}` at `if:` condition is tricky. When the expression in `${{ }}` is evaluated to boolean value and there is
no extra characters around the `${{ }}`, the condition is evaluated to the boolean value. Otherwise, the condition is treated as
string hence it is **always** evaluated to `true`.

It means that multi-line string must not be used at `if:` condition (`if: |`) because the condition is always evaluated to true.
Multi-line string inserts newline character at end of each line.

```yaml
if: |
  ${{ false }}
```

is equivalent to

```yaml
if: "${{ false }}\n"
```

Unlike using `${{ }}`, putting an expression directly ignores white spaces around it. It's the reason why

```yaml
if: |
  false
```

works as intended.

actionlint checks all `if:` conditions in workflow and reports error when some condition is always evaluated to true due to extra
characters around `${{ }}`.

<a id="action-metadata-syntax"></a>
## Action metadata syntax validation

Example action metadata:

```yaml
# .github/actions/my-invalid-action/action.yml

name: 'My action'
author: '...'
# ERROR: 'description' section is required

branding:
  # ERROR: Invalid icon name
  icon: dog
  # ERROR: Unsupported icon color
  color: black

runs:
  # ERROR: Node.js runtime version is too old
  using: 'node14'
  # ERROR: The source file being run by this action does not exist
  main: 'this-file-does-not-exist.js'
  # ERROR: 'env' configuration is only allowed for Docker actions
  env:
    SOME_VAR: SOME_VALUE
```

Example input:

```yaml
on: push

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      # actionlint checks an action when it is actually used in a workflow
      - uses: ./.github/actions/my-invalid-action
```

Output:
<!-- Skip update output -->

```
action_metadata_syntax_validation.yaml:8:15: description is required in metadata of "My action" action at "path/to/.github/actions/my-invalid-action/action.yml" [action]
  |
8 |       - uses: ./.github/actions/my-invalid-action
  |               ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
action_metadata_syntax_validation.yaml:8:15: incorrect icon name "dog" at branding.icon in metadata of "My action" action at "path/to/.github/actions/my-invalid-action/action.yml". see the official document to know the exhaustive list of supported icons: https://docs.github.com/en/actions/creating-actions/metadata-syntax-for-github-actions#brandingicon [action]
  |
8 |       - uses: ./.github/actions/my-invalid-action
  |               ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
action_metadata_syntax_validation.yaml:8:15: incorrect color "black" at branding.icon in metadata of "My action" action at "path/to/.github/actions/my-invalid-action/action.yml". see the official document to know the exhaustive list of supported colors: https://docs.github.com/en/actions/creating-actions/metadata-syntax-for-github-actions#brandingcolor [action]
  |
8 |       - uses: ./.github/actions/my-invalid-action
  |               ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
action_metadata_syntax_validation.yaml:8:15: invalid runner name "node14" at runs.using in "My action" action defined at "path/to/.github/actions/my-invalid-action". valid runners are "composite", "docker", "node16", and "node20". see https://docs.github.com/en/actions/creating-actions/metadata-syntax-for-github-actions#runs [action]
  |
8 |       - uses: ./.github/actions/my-invalid-action
  |               ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
action_metadata_syntax_validation.yaml:8:15: file "this-file-does-not-exist.js" does not exist in "path/to/.github/actions/my-invalid-action". it is specified at "main" key in "runs" section in "My action" action [action]
  |
8 |       - uses: ./.github/actions/my-invalid-action
  |               ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
action_metadata_syntax_validation.yaml:8:15: "env" is not allowed in "runs" section because "My action" is a JavaScript action. the action is defined at "path/to/.github/actions/my-invalid-action" [action]
  |
8 |       - uses: ./.github/actions/my-invalid-action
  |               ^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
```

<!-- Skip playground link -->

All actions require a metadata file `action.yml` or `action.yaml`. The syntax is defined in [the official document][action-metadata-doc].

actionlint checks metadata files used in workflows and reports errors when they are not following the syntax.

- `name:`, `description:`, `runs:` sections are required
- Runner name at `using:` is one of `composite`, `docker`, `node16`, `node20`
- Keys under `runs:` section are correct. Required/Valid keys are different depending on the type of action; Docker action or
  Composite action or JavaScript action (e.g. `image:` is required for Docker action).
- Files specified in some keys under `runs` are existing. For example, JavaScript action defines a script file path for
  entrypoint at `main:`.
- Icon name at `icon:` in `branding:` section is correct. Supported icon names are listed in
  [the official document][branding-icons-doc].
- Icon color at `color:` in `branding:` section is correct. Supported icon colors are white, yellow, blue, green, orange, red,
  purple, or gray-dark.

actionlint checks action metadata files which are used by workflows. Currently, it is not supported to specify `action.yml`
directly via command line arguments.

Note that `steps` in Composite action's metadata is not checked at this point. It will be supported in the future.

---

[Installation](install.md) | [Usage](usage.md) | [Configuration](config.md) | [Go API](api.md) | [References](reference.md)

[yamllint]: https://github.com/adrienverge/yamllint
[issue-form]: https://github.com/rhysd/actionlint/issues/new
[syntax-doc]: https://docs.github.com/en/actions/learn-github-actions/workflow-syntax-for-github-actions
[filter-pattern-doc]: https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions#filter-pattern-cheat-sheet
[shellcheck]: https://github.com/koalaman/shellcheck
[shellcheck-install]: https://github.com/koalaman/shellcheck#installing
[SC1091]: https://github.com/koalaman/shellcheck/wiki/SC1091
[SC2050]: https://github.com/koalaman/shellcheck/wiki/SC2050
[SC2194]: https://github.com/koalaman/shellcheck/wiki/SC2194
[SC2154]: https://github.com/koalaman/shellcheck/wiki/SC2154
[SC2157]: https://github.com/koalaman/shellcheck/wiki/SC2157
[SC2043]: https://github.com/koalaman/shellcheck/wiki/SC2043
[shellcheck-env-var]: https://github.com/koalaman/shellcheck/wiki/Integration#environment-variables
[pyflakes]: https://github.com/PyCQA/pyflakes
[expr-doc]: https://docs.github.com/en/actions/learn-github-actions/expressions
[contexts-doc]: https://docs.github.com/en/actions/learn-github-actions/contexts
[funcs-doc]: https://docs.github.com/en/actions/learn-github-actions/expressions#functions
[needs-doc]: https://docs.github.com/en/actions/learn-github-actions/workflow-syntax-for-github-actions#jobsjob_idneeds
[needs-context-doc]: https://docs.github.com/en/actions/learn-github-actions/contexts#needs-context
[shell-doc]: https://docs.github.com/en/actions/learn-github-actions/workflow-syntax-for-github-actions#using-a-specific-shell
[matrix-doc]: https://docs.github.com/en/actions/learn-github-actions/workflow-syntax-for-github-actions#jobsjob_idstrategymatrix
[webhook-doc]: https://docs.github.com/en/actions/learn-github-actions/events-that-trigger-workflows#webhook-events
[schedule-event-doc]: https://docs.github.com/en/actions/learn-github-actions/events-that-trigger-workflows#scheduled-events
[cron-syntax]: https://pubs.opengroup.org/onlinepubs/9699919799/utilities/crontab.html#tag_20_25_07
[gh-hosted-runner]: https://docs.github.com/en/actions/using-github-hosted-runners/about-github-hosted-runners
[self-hosted-runner]: https://docs.github.com/en/actions/hosting-your-own-runners/about-self-hosted-runners
[action-uses-doc]: https://docs.github.com/en/actions/learn-github-actions/workflow-syntax-for-github-actions#jobsjob_idstepsuses
[dependabot-doc]: https://docs.github.com/en/code-security/dependabot/working-with-dependabot/keeping-your-actions-up-to-date-with-dependabot
[credentials-doc]: https://docs.github.com/en/actions/learn-github-actions/workflow-syntax-for-github-actions#jobsjob_idcontainercredentials
[actions-cache]: https://github.com/actions/cache
[permissions-doc]: https://docs.github.com/en/actions/security-guides/automatic-token-authentication#permissions-for-the-github_token
[perm-config-doc]: https://docs.github.com/en/actions/learn-github-actions/workflow-syntax-for-github-actions#permissions
[generate-webhook-events]: https://github.com/rhysd/actionlint/tree/main/scripts/generate-webhook-events
[generate-popular-actions]: https://github.com/rhysd/actionlint/tree/main/scripts/generate-popular-actions
[issue-25]: https://github.com/rhysd/actionlint/issues/25
[issue-40]: https://github.com/rhysd/actionlint/issues/40
[security-doc]: https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions
[reusable-workflow-doc]: https://docs.github.com/en/actions/learn-github-actions/reusing-workflows
[create-reusable-workflow-doc]: https://docs.github.com/en/actions/learn-github-actions/reusing-workflows#creating-a-reusable-workflow
[reusable-workflow-call-keys]: https://docs.github.com/en/actions/learn-github-actions/reusing-workflows#supported-keywords-for-jobs-that-call-a-reusable-workflow
[object-filter-syntax]: https://docs.github.com/en/actions/learn-github-actions/expressions#object-filters
[github-script]: https://github.com/actions/github-script
[workflow-dispatch-event]: https://docs.github.com/en/actions/learn-github-actions/events-that-trigger-workflows#workflow_dispatch
[workflow-dispatch-input-type-announce]: https://github.blog/changelog/2021-11-10-github-actions-input-types-for-manual-workflows/
[reusable-workflow-outputs]: https://docs.github.com/en/actions/using-workflows/reusing-workflows#using-outputs-from-a-reusable-workflow
[inherit-secrets-announce]: https://github.blog/changelog/2022-05-03-github-actions-simplify-using-secrets-with-reusable-workflows/
[specific-paths-doc]: https://docs.github.com/en/actions/using-workflows/triggering-a-workflow#using-filters-to-target-specific-paths-for-pull-request-or-push-events
[availability-doc]: https://docs.github.com/en/actions/learn-github-actions/contexts#context-availability
[deprecate-set-output-save-state]: https://github.blog/changelog/2022-10-11-github-actions-deprecating-save-state-and-set-output-commands/
[deprecate-set-env-add-path]: https://github.blog/changelog/2020-10-01-github-actions-deprecating-set-env-and-add-path-commands/
[workflow-commands-doc]: https://docs.github.com/en/actions/using-workflows/workflow-commands-for-github-actions
[action-metadata-doc]: https://docs.github.com/en/actions/creating-actions/metadata-syntax-for-github-actions
[branding-icons-doc]: https://github.com/github/docs/blob/main/content/actions/creating-actions/metadata-syntax-for-github-actions.md#exhaustive-list-of-all-currently-supported-icons
[operators-doc]: https://docs.github.com/en/actions/learn-github-actions/expressions#operators
