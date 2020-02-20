# -*- coding: utf-8 -*-
"""
    Lexer for the GOR programming language.
    
    In order for Sphinx to be able to locate this lexer it needs to be added to 
    Pygments. That is done by saving it here: 
    /opt/local/Library/Frameworks/Python.framework/Versions/2.7/lib/python2.7/site-packages/pygments/lexers
    and then running the script "_mapping.py" like so:
    python _mapping.py

"""

import re

from pygments.lexer import Lexer, RegexLexer
from pygments.token import Text, Keyword, Name



__all__ = ['GORLexer']

class GORLexer(RegexLexer):
    """

    """

    name = 'GOR'
    aliases = ['gor']
    filenames = ['*.gor']
    mimetypes = ['text/x-gor']

    tokens = {
        'root': [
            (r'\.gor', Text),                                                                       #Don't want to highlight file extensions
            (r'\b(gor|nor|pgor|create|def|gorcmd|gorsql|norcmd|norsql)\b', Name.Decorator),
            (r'\b(IF|-len|str|int|float|listfilter)\b', Keyword),                                   #Render key words blue
            (r'\b(BAMFLAG|BASES|BUCKETSPLIT|CALC|CIGARSEGS|CMD|COLNUM|COLSPLIT|COLUMNSORT|CSVCC|'
             r'CSVSEL|DAGMAP|DISTLOC|GAVA|GOR|GORROW|GRANNO|GREP|GROUP|HIDE|INSET|'
             r'JOIN|LEFTJOIN|LEFTWHERE|LIFTOVER|LOG|MAP|MERGE|MULTIMAP|NOR|PARTGOR|PEDPIVOT|PGOR|PILEUP|'
             r'PIVOT|PREFIX|RANK|RENAME|REPLACE|ROWNUM|SED|SEGPROJ|SEGSPAN|SELECT|SEQ|SKIP|'
             r'SORT|SPLIT|SQL|TEE|THROWIF|TOP|TRYCALC|TRYHIDE|TRYSELECT|TRYWHERE|UNPIVOT|UNTIL|'
             r'VARIANTS|VARJOIN|VARMERGE|VARNORM|VERIFYORDER|WAIT|WHERE|WRITE)\b', Name.Builtin),        #Render commands purple
            (r'\b(p|snpseg|segsnp|snpsnp|f|count|max|min|avg|gc|ic)\b', Name.Constant),             #Render attributes teal
            (r'\b(AND|IF|NOT)\b', Name.Entity),                                                         #Render attributes pink
            (r'\b(Gene_Symbol|gene_symbol|Chrom|POS|pos|call|reference|allele|alt|'
             r'distance|Reference|Call|FILTER|PN|PID|allCount|rsIDs|'
             r'Max_Impact|chromstart|chromend|min_chromstart|max_chromend)\b', Name.Attribute),                                                     #Render column names blue
            (r'[^\s]', Text),                                                                       #If no keyword found scan until next whitespace and categorize as Text token(no highilighting)
            (r'\s', Text),                                                                          #The whitespace it self is also categorized as a Text token
        ],
    }
