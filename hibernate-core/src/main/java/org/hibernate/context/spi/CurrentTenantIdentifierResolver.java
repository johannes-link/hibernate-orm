/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.context.spi;

/**
 * A callback registered with the {@link org.hibernate.SessionFactory} that is
 * responsible for resolving the current tenant identifier.
 * <p>
 * An implementation may be selected by setting the configuration property
 * {@value org.hibernate.cfg.AvailableSettings#MULTI_TENANT_IDENTIFIER_RESOLVER}.
 *
 * @see org.hibernate.cfg.Configuration#setCurrentTenantIdentifierResolver
 * @see org.hibernate.boot.SessionFactoryBuilder#applyCurrentTenantIdentifierResolver
 * @see org.hibernate.annotations.TenantId
 * @see org.hibernate.cfg.AvailableSettings#MULTI_TENANT_IDENTIFIER_RESOLVER
 *
 * @author Steve Ebersole
 */
public interface CurrentTenantIdentifierResolver {
	/**
	 * Resolve the current tenant identifier.
	 * 
	 * @return The current tenant identifier
	 */
	String resolveCurrentTenantIdentifier();

	/**
	 * Should we validate that the tenant identifier of a "current sessions" that
	 * already exists when {@link CurrentSessionContext#currentSession()} is called
	 * matches the value returned here from {@link #resolveCurrentTenantIdentifier()}?
	 * 
	 * @return {@code true} indicates that the extra validation will be performed;
	 *                      {@code false} indicates it will not.
	 *
	 * @see org.hibernate.context.TenantIdentifierMismatchException
	 */
	boolean validateExistingCurrentSessions();

	/**
	 * Does the given tenant id represent a "root" tenant with access to all partitions?
	 *
	 * @param tenantId a tenant id produced by {@link #resolveCurrentTenantIdentifier()}
	 *
	 * @return true is this is root tenant
	 */
	default boolean isRoot(String tenantId) {
		return false;
	}
}
