class MJC {
    static void putc(char s);
    static CPOINTER allocObject(int bytes);
    static CPOINTER allocArray(int bytes, int len);
    static CPOINTER arrayIndex(CPOINTER array, int index);
    static void die();
}

class Exception {
    public static void throw (String msg) {
        System.out.print("EXCEPTION: ");
        System.out.println(msg);
        MJC.die();
    }
    public static void throwLoc(String file, int lineno, String message) {
        String [] items = new String[3];
        items[0] = file;
        items[1] = Integer.toString(lineno);
        items[2] = message;
        Exception.throw(String.format("%:% %", items));
    }
}

class Object {
    public String toString() {
        return "<SomeObject>";
    }
}

class Array {
    public static boolean inBounds(int length, int index) {
        return index < length && index > -1;
    }
    public static boolean boundsCheck(String filename, int lineno, int length,
                                      int index) {
        if (inBounds(length, index)) {
            return true;
        } else {
            String msg = "";
            String [] items = new String[2];
            items[0] = Integer.toString(index);
            items[1] = Integer.toString(length);
            Exception.throwLoc(filename, lineno,
                               String.format("Array access out of bounds (index %, length %).", items));
            return false;
        }
    }
}

class String {
    private char [] string;
    public int length() {
        return string.length;
    }
    public static String makeStringChar(char [] c) {
        int x;
        String s;
        s = new String();
        s.string = new char[c.length];
        x = 0;
        while (x < c.length) {
            s.string[x] = c[x];
            x = x + 1;
        }
        return s;
    }
    public String toString() {
        return this;
    }
    public static String format(String format, String [] items) {
        String result = "";
        int x = 0;
        int current_replace = 0;
        while (x < format.length()) {
            char c = format.charAt(x);
            if (c == '%') {
                if (current_replace < items.length) {
                    result = result.append(items[current_replace]);
                    current_replace = current_replace + 1;
                } else {
                    Exception.throw("Too few strings provided to format.");
                }
            } else {
                result = result.appendChar(c);
            }
            x = x + 1;
        }
        return result;
    }
    public String appendChar(char c) {
        char [] result = new char[this.length() + 1];
        int x = 0;
        while (x < this.length()) {
            result[x] = this.string[x];
            x = x + 1;
        }
        result[x] = c;
        return String.makeStringChar(result);
    }

    public String append(String s) {
        String appended;
        int new_length;
        new_length = s.length() + this.length();
        appended = new String();
        appended.string = new char[new_length];
        int x;
        x = 0;
        while (x < this.length()) {
            appended.string[x] = this.string[x];
            x = x + 1;
        }
        x = 0;
        while (x < s.length()) {
            appended.string[x + this.length()] = s.string[x];
            x = x + 1;
        }
        return appended;
    }

    public char charAt(int x) {
        return this.string[x];
    }
}

class PrintStream {
    public static void println(Object o) {
        print(o);
        print("\n");
    }
    public static void print(Object o) {
        String s;
        s = o.toString();
        int x;
        x = 0;
        while (x < s.length()) {
            MJC.putc(s.charAt(x));
            x = x + 1;
        }
    }
}

class Integer {
    public static char digitToChar(int x) {
        if (x == 0) {
            return '0';
        } else if (x == 1) {
            return '1';
        } else if (x == 2) {
            return '2';
        } else if (x == 3) {
            return '3';
        } else if (x == 4) {
            return '4';
        } else if (x == 5) {
            return '5';
        } else if (x == 6) {
            return '6';
        } else if (x == 7) {
            return '7';
        } else if (x == 8) {
            return '8';
        } else if (x == 9) {
            return '9';
        } else {
            System.out.println("Digit not between 0-9");
        }
        return 'X';
    }
    public static String toString(int convert) {
        int size = 0;
        int num = convert;
        boolean neg = false;
        if (num < 0) {
            neg = true;
            num = 0 - num;
        }
        int pos = num;
        while (num > 0) {
            num = num / 10;
            size = size + 1;
        }
        /* minimum digit size should be at least 1 (for a zero) */
        if (size < 1) {
            size = 1;
        }
        if (neg) {
            size = size + 1;
        }
        char [] number = new char[size];

        int x = size - 1;
        num = pos;
        while (x > 0 || x == 0) {
            int digit = num % 10;
            num = num / 10;
            char c_digit = Integer.digitToChar(digit);
            number[x] = c_digit;
            x = x - 1;
        }
        if (neg) {
            number[0] = '-';
        }
        return String.makeStringChar(number);
    }
}

class System {
    public static PrintStream out = new PrintStream();
}

