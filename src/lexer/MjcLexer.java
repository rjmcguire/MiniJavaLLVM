/*
 * MiniJava Compiler - X86, LLVM Compiler/Interpreter for MiniJava.
 * Copyright (C) 2014, 2008 Mitch Souders, Mark A. Smith, Mark P. Jones
 *
 * MiniJava Compiler is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * MiniJava Compiler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MiniJava Compiler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */


package lexer;

import java.util.Hashtable;
import compiler.Source;
import compiler.SourceLexer;
import compiler.Handler;
import compiler.Warning;
import compiler.Failure;
import syntax.Id;
import syntax.IntLiteral;
import syntax.StringLiteral;
import syntax.CharLiteral;
import syntax.Tokens;

/** A lexical analyzer for the mini Java compiler.
 */
public class MjcLexer extends SourceLexer implements Tokens {
    /** Construct a lexical analyzer for a mini Java compiler.
     */
    public MjcLexer(Handler handler, Source source) {
        super(handler, source);
    }

    /** Read the next token and return the corresponding integer code.
     */
    public int nextToken() {
        for (;;) {
            skipWhitespace();
            markPosition();
            lexemeText = null;
            semantic   = null;
            switch (c) {
            case EOF  :
                return token = ENDINPUT;

                // Separators:
            case '('  :
                nextChar();
                return token = '(';
            case ')'  :
                nextChar();
                return token = ')';
            case '[' :
                nextChar();
                if (c == ']') {
                    nextChar();
                    return token = ARRAY;
                } else {
                    return token = '[';
                }
            case ']':
                nextChar();
                return token = ']';
            case '{'  :
                nextChar();
                return token = '{';
            case '}'  :
                nextChar();
                return token = '}';
            case ';'  :
                nextChar();
                return token = ';';
            case ','  :
                nextChar();
                return token = ',';
            case '.'  :
                nextChar();
                if (Character.isDigit((char)c)) {
                    backStep();
                    return number();
                }
                return token = '.';

                // Operators:
            case '='  :
                nextChar();
                if (c == '=') {
                    nextChar();
                    return token = EQEQ;
                } else {
                    return token = '=';
                }

            case '>'  :
                nextChar();
                return token = '>';

            case '<'  :
                nextChar();
                return token = '<';

            case '!'  :
                nextChar();
                if (c == '=') {
                    nextChar();
                    return token = NEQ;
                } else {
                    return token = '!';
                }

            case '&'  :
                nextChar();
                if (c == '&') {
                    nextChar();
                    return token = CAND;
                } else {
                    return token = '&';
                }

            case '|'  :
                nextChar();
                if (c == '|') {
                    nextChar();
                    return token = COR;
                } else {
                    return token = '|';
                }

            case '^'  :
                nextChar();
                return token = '^';

            case '+'  :
                nextChar();
                return token = '+';

            case '-'  :
                nextChar();
                return token = '-';
            case '*'  :
                nextChar();
                return token = '*';
            case '%' :
                nextChar();
                return token = '%';
            case '/'  :
                nextChar();
                if (c == '/') {
                    skipOneLineComment();
                } else if (c == '*') {
                    skipBracketComment();
                } else {
                    return token = '/';
                }
                break;
            case '"':
                return string();
            case '\'':
                nextChar();
                if (c == '\\') {
                    semantic = new CharLiteral(getPos(), escapeSeq());
                } else {
                    semantic = new CharLiteral(getPos(), (char)c);
                }
                nextChar();
                if (c != '\'') {
                    report(new Failure("Incorrectly terminated char literal"));
                }
                nextChar();
                return token = CHARLIT;
            default   :
                if (Character.isJavaIdentifierStart((char)c)) {
                    return identifier();
                } else if (Character.digit((char)c, 10) >= 0) {
                    return number();
                } else {
                    illegalCharacter();
                    nextChar();
                }
            }
        }
    }

    //- Whitespace and comments -----------------------------------------------

    private boolean isWhitespace(int c) {
        return (c == ' ') || (c == '\t') || (c == '\f');
    }

    private void skipWhitespace() {
        while (isWhitespace(c)) {
            nextChar();
        }
        while (c == EOL) {
            nextLine();
            while (isWhitespace(c)) {
                nextChar();
            }
        }
    }

    private void skipOneLineComment() { // Assumes c=='/'
        nextLine();
    }

    private void skipBracketComment() { // Assumes c=='*'
        nextChar();
        for (;;) {
            if (c == '*') {
                do {
                    nextChar();
                } while (c == '*');
                if (c == '/') {
                    nextChar();
                    return;
                }
            }
            if (c == EOF) {
                report(new Failure(getPos(), "Unterminated comment"));
                return;
            }
            if (c == EOL) {
                nextLine();
            } else {
                nextChar();
            }
        }
    }

    private char escapeSeq() {
        nextChar();
        switch (c) {
        case 'n':
            return '\n';
        case '\\':
            return '\\';
        case 't':
            return '\t';
        case '"':
            return '\"';
        case '\'':
            return '\'';
        default:
            report(new Failure(getPos(),
                               "Unknown string escape sequence: \\" + (char)c));
            break;
        }
        return '\0';
    }
    private int string() {
        int start = col;
        StringBuilder b = new StringBuilder();
        boolean endOfString = false;
        do {
            nextChar();
            switch ((char)c) {
            case '"':
                endOfString = true;
                break;
            case '\\':
                b.append(escapeSeq());
                break;
            default:
                b.append((char)c);
                break;
            }
        } while (c != EOF && !endOfString);
        if (c == EOF) {
            report(new Failure(getPos(),
                               "Unterminated string literal"));
        }
        nextChar(); // consume closing "
        semantic = new StringLiteral(getPos(), b.toString());
        return token = STRING;
    }

    //- Identifiers, keywords, boolean and null literals ----------------------

    private int identifier() {          // Assumes isJavaIdentifierStart(c)
        int start = col;
        do {
            nextChar();
        } while (c != EOF && Character.isJavaIdentifierPart((char)c));
        lexemeText = line.substring(start, col);

        Object kw  = reserved.get(lexemeText);
        if (kw != null) {
            return token = ((Integer)kw).intValue();
        }
        semantic = new Id(getPos(), lexemeText);
        return token = IDENT;
    }

    private static Hashtable reserved;
    static {
        reserved = new Hashtable();
        reserved.put("boolean", new Integer(BOOLEAN));
        reserved.put("class",   new Integer(CLASS));
        reserved.put("else",    new Integer(ELSE));
        reserved.put("extends", new Integer(EXTENDS));
        reserved.put("if",      new Integer(IF));
        reserved.put("int",     new Integer(INT));
        reserved.put("char",    new Integer(CHAR));
        reserved.put("new",     new Integer(NEW));
        reserved.put("return",  new Integer(RETURN));
        reserved.put("static",    new Integer(STATIC));
        reserved.put("public",    new Integer(PUBLIC));
        reserved.put("private",   new Integer(PRIVATE));
        reserved.put("protected", new Integer(PROTECTED));
        reserved.put("abstract",  new Integer(ABSTRACT));
        reserved.put("super",   new Integer(SUPER));
        reserved.put("this",    new Integer(THIS));
        reserved.put("void",    new Integer(VOID));
        reserved.put("while",   new Integer(WHILE));
        reserved.put("do",      new Integer(DO));
        reserved.put("null",    new Integer(NULL));
        reserved.put("true",    new Integer(TRUE));
        reserved.put("false",   new Integer(FALSE));
        reserved.put("CPOINTER",   new Integer(PTR));
        reserved.put("interface",   new Integer(INTERFACE));
        reserved.put("implements",   new Integer(IMPLEMENTS));
    }

    //- Numeric integer literals ----------------------------------------------

    private int number() {
        // We defer computing the value of the intliteral until later
        // to allow min and max int values (minus sign may be added later).
        StringBuilder b = new StringBuilder();
        do {
            b.append((char)c);
            nextChar();
        } while ((char)c >= '0' && (char)c <= '9');
        semantic = new IntLiteral(getPos(), b.toString());
        return token = INTLIT;
    }

    //- Miscellaneous support functions ---------------------------------------

    private Object semantic  = null;

    /** Return the semantic value of the current token.  If no explicit
     *  semantic value has been provided, then we substitute the current
     *  position.
     */
    public Object getSemantic() {
        if (semantic == null) {
            semantic = getPos();
        }
        return semantic;
    }

    private void backStep() {
        if (col > 0) {
            c = line.charAt(--col);
        }
    }

    private void illegalCharacter() {
        report(new Warning(getPos(), "Ignoring illegal character '" + c + "'"));
    }
}
