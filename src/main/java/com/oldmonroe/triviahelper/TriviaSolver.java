package com.oldmonroe.triviahelper;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure logic - no Minecraft classes referenced, so this is easy to unit test
 * outside the game if you ever want to.
 *
 * Handles three things:
 *   1. Roman numeral  -> number      (MMDCCXXXVI -> 2736)
 *   2. Number         -> Roman       (2736 -> MMDCCXXXVI)
 *   3. Arithmetic expression         (17 * 3 + 4 -> 55)
 */
public final class TriviaSolver {
	private TriviaSolver() {
	}

	/** A well-formed Roman numeral in canonical (subtractive) notation. */
	private static final Pattern ROMAN_STRICT = Pattern.compile(
			"^M{0,3}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$");

	/** Loose match used to pull a numeral out of a sentence. */
	private static final Pattern ROMAN_IN_TEXT = Pattern.compile("\\b[MDCLXVI]{2,}\\b");

	private static final Pattern PLAIN_INTEGER = Pattern.compile("^\\d{1,7}$");
	private static final Pattern INTEGER_IN_TEXT = Pattern.compile("\\b(\\d{1,7})\\b");
	private static final Pattern MATH_IN_TEXT = Pattern.compile("[-+]?[\\d.]+(\\s*[-+*/%^]\\s*[-+]?[\\d.()]+)+");

	private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
	private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

	/**
	 * @param answer      the bare answer, ready to be pasted
	 * @param explanation a short human-readable note, or "" if there is nothing useful to say
	 * @param ok          whether anything was actually solved
	 */
	public record Result(String answer, String explanation, boolean ok) {
		static Result fail(String why) {
			return new Result("", why, false);
		}
	}

	/**
	 * Solves whatever the user typed. Tries, in order: Roman numeral, plain
	 * integer, arithmetic expression, then falls back to digging a candidate
	 * out of surrounding sentence text.
	 */
	public static Result solve(String rawInput) {
		if (rawInput == null) {
			return Result.fail("Nothing to solve.");
		}

		String input = rawInput.trim();

		if (input.isEmpty()) {
			return Result.fail("Nothing to solve.");
		}

		// 1. Bare Roman numeral.
		String upper = input.toUpperCase(java.util.Locale.ROOT);

		if (isRoman(upper)) {
			int value = romanToArabic(upper);
			return new Result(Integer.toString(value), upper + " = " + value, true);
		}

		// 2. Bare integer -> Roman.
		if (PLAIN_INTEGER.matcher(input).matches()) {
			int value = Integer.parseInt(input);

			if (value >= 1 && value <= 3999) {
				String roman = arabicToRoman(value);
				return new Result(roman, value + " = " + roman, true);
			}

			return Result.fail("Roman numerals only cover 1-3999 without overline notation.");
		}

		// 3. Arithmetic expression.
		if (looksLikeMath(input)) {
			try {
				BigDecimal value = new ExpressionParser(input).parse();
				String formatted = format(value);
				return new Result(formatted, input + " = " + formatted, true);
			} catch (ArithmeticException e) {
				return Result.fail("Math error: " + e.getMessage());
			} catch (RuntimeException e) {
				// Fall through to the sentence-scraping pass below.
			}
		}

		// 4. It is probably a full question sentence. Pull something out of it.
		String candidate = extractCandidate(input);

		if (candidate != null && !candidate.equalsIgnoreCase(input)) {
			Result nested = solve(candidate);

			if (nested.ok()) {
				return nested;
			}
		}

		return Result.fail("Could not parse that. Try just the numeral, number, or expression.");
	}

	/**
	 * Pulls the most likely thing-to-solve out of a full sentence such as
	 * "What number is the Roman numeral MMDCCXXXVI?".
	 */
	public static String extractCandidate(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}

		// A Roman numeral is the most distinctive thing, so look for it first.
		// Guard against matching ordinary words: require it to be all-caps in
		// the original text and to be a valid numeral.
		Matcher roman = ROMAN_IN_TEXT.matcher(text);

		while (roman.find()) {
			String found = roman.group();

			if (isRoman(found) && found.equals(found.toUpperCase(java.util.Locale.ROOT))) {
				return found;
			}
		}

		Matcher math = MATH_IN_TEXT.matcher(text);

		if (math.find()) {
			return math.group().trim();
		}

		// "Write 2736 as a Roman numeral" style questions.
		if (text.toLowerCase(java.util.Locale.ROOT).contains("roman")) {
			Matcher number = INTEGER_IN_TEXT.matcher(text);

			if (number.find()) {
				return number.group(1);
			}
		}

		return null;
	}

	public static boolean isRoman(String s) {
		if (s == null || s.isEmpty()) {
			return false;
		}

		return ROMAN_STRICT.matcher(s.toUpperCase(java.util.Locale.ROOT)).matches();
	}

	public static int romanToArabic(String roman) {
		String s = roman.toUpperCase(java.util.Locale.ROOT);
		int total = 0;
		int previous = 0;

		for (int i = s.length() - 1; i >= 0; i--) {
			int current = charValue(s.charAt(i));

			if (current < previous) {
				total -= current;
			} else {
				total += current;
				previous = current;
			}
		}

		return total;
	}

	public static String arabicToRoman(int number) {
		if (number < 1 || number > 3999) {
			throw new IllegalArgumentException("out of range");
		}

		StringBuilder out = new StringBuilder();
		int remaining = number;

		for (int i = 0; i < VALUES.length; i++) {
			while (remaining >= VALUES[i]) {
				out.append(SYMBOLS[i]);
				remaining -= VALUES[i];
			}
		}

		return out.toString();
	}

	private static int charValue(char c) {
		return switch (c) {
			case 'I' -> 1;
			case 'V' -> 5;
			case 'X' -> 10;
			case 'L' -> 50;
			case 'C' -> 100;
			case 'D' -> 500;
			case 'M' -> 1000;
			default -> 0;
		};
	}

	private static boolean looksLikeMath(String s) {
		return s.matches("[-+*/%^().\\d\\s]+") && s.matches(".*\\d.*");
	}

	private static String format(BigDecimal value) {
		BigDecimal stripped = value.stripTrailingZeros();

		if (stripped.scale() <= 0) {
			return stripped.toBigInteger().toString();
		}

		return stripped.toPlainString();
	}

	/**
	 * Small recursive-descent parser.
	 * Grammar (lowest to highest precedence):
	 *   expression := term (('+' | '-') term)*
	 *   term       := factor (('*' | '/' | '%') factor)*
	 *   factor     := unary ('^' factor)?        [right associative]
	 *   unary      := ('-' | '+')? primary
	 *   primary    := number | '(' expression ')'
	 */
	private static final class ExpressionParser {
		private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

		private final String src;
		private int pos;

		ExpressionParser(String src) {
			this.src = src;
			this.pos = 0;
		}

		BigDecimal parse() {
			BigDecimal value = expression();
			skipSpaces();

			if (pos < src.length()) {
				throw new IllegalArgumentException("unexpected '" + src.charAt(pos) + "'");
			}

			return value;
		}

		private BigDecimal expression() {
			BigDecimal left = term();

			while (true) {
				skipSpaces();

				if (eat('+')) {
					left = left.add(term(), MC);
				} else if (eat('-')) {
					left = left.subtract(term(), MC);
				} else {
					return left;
				}
			}
		}

		private BigDecimal term() {
			BigDecimal left = factor();

			while (true) {
				skipSpaces();

				if (eat('*')) {
					left = left.multiply(factor(), MC);
				} else if (eat('/')) {
					BigDecimal divisor = factor();

					if (divisor.signum() == 0) {
						throw new ArithmeticException("division by zero");
					}

					left = left.divide(divisor, MC);
				} else if (eat('%')) {
					BigDecimal divisor = factor();

					if (divisor.signum() == 0) {
						throw new ArithmeticException("division by zero");
					}

					left = left.remainder(divisor, MC);
				} else {
					return left;
				}
			}
		}

		private BigDecimal factor() {
			BigDecimal base = unary();
			skipSpaces();

			if (eat('^')) {
				BigDecimal exponent = factor();

				try {
					return base.pow(exponent.intValueExact(), MC);
				} catch (ArithmeticException e) {
					throw new ArithmeticException("exponent must be a whole number");
				}
			}

			return base;
		}

		private BigDecimal unary() {
			skipSpaces();

			if (eat('-')) {
				return unary().negate();
			}

			if (eat('+')) {
				return unary();
			}

			return primary();
		}

		private BigDecimal primary() {
			skipSpaces();

			if (eat('(')) {
				BigDecimal inner = expression();
				skipSpaces();

				if (!eat(')')) {
					throw new IllegalArgumentException("missing ')'");
				}

				return inner;
			}

			int start = pos;

			while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
				pos++;
			}

			if (start == pos) {
				throw new IllegalArgumentException("expected a number");
			}

			return new BigDecimal(src.substring(start, pos));
		}

		private void skipSpaces() {
			while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
				pos++;
			}
		}

		private boolean eat(char c) {
			skipSpaces();

			if (pos < src.length() && src.charAt(pos) == c) {
				pos++;
				return true;
			}

			return false;
		}
	}
}
