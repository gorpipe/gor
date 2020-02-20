# generate-getsignature

This script generates the FunctionSignature scala object used for function registration
for the ParseArith parser that parses the expressions used in CALC and WHERE commands.

```bash
python3 generate-getsignature.py > FunctionSignature.scala
```

A generic getSignature function requires TypeTags, which unfortunately incur a startup
cost of 2.5 seconds or so. We often use GorPipe as a command line tool and we'd rather not
accept that extra cost on every startup, so I've resorted to generating functions for
getting a signature for every possible combination of parameters.

The parser recognizes the following types:
* Int
* Double
* Long
* String
* Boolean

Functions can have from 0 to 4 parameters, and have a return type, so the number of
variants is 5^0 + 5^1 + 5^2 + 5^3 + 5^4 + 5^5, or 3905. Admittedly we only use a small
percentage of those variants, but when offering a generic registration mechanism it
is important to be able to support any combination of parameters.