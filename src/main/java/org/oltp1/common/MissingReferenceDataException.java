package org.oltp1.common;

/**
 * Thrown when a transaction needs a reference row that the database does not have.
 *
 * <p>These are data conditions, not programming errors, and they are reachable in normal runs: a
 * security with no {@code LAST_TRADE} row, a customer with no {@code CUSTOMER_TAXRATE} row, a
 * missing {@code TRADE_TYPE} or fee row. Left to a bare {@code executeScalar(...)} unboxing they
 * surface as a {@link NullPointerException} or a {@code NoSuchElementException} that names
 * neither the query nor the key that was missing, and the transaction is then counted as a
 * generic error.
 *
 * <p>A transaction that raises this rolls back like any other failure. It is
 * <em>not</em> retryable: the row will not appear on a second attempt, so a retry policy for
 * transient errors (serialization failures, lock timeouts) must exclude it.
 */
public class MissingReferenceDataException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	private final String reference;

	public MissingReferenceDataException(String reference, String detail)
	{
		super(reference + " -> " + detail);
		this.reference = reference;
	}

	/** The reference that was missing, e.g. {@code "LAST_TRADE row for symbol ACME"}. */
	public String getReference()
	{
		return reference;
	}
}
