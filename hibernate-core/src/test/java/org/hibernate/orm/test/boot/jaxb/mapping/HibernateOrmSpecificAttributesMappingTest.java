/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Types;
import java.util.Set;
import java.util.UUID;

import org.hibernate.boot.model.process.internal.UserTypeResolution;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.internal.TenantIdGeneration;
import org.hibernate.id.uuid.UuidGenerator;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.GeneratorCreator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.usertype.UserTypeSupport;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

@DomainModel(annotatedClasses = HibernateOrmSpecificAttributesMappingTest.MyEntity.class, xmlMappings = "xml/jaxb/mapping/partial/hibernate-orm-specific-attributes.xml")
public class HibernateOrmSpecificAttributesMappingTest {
	@Test
	public void verifyMapping(DomainModelScope scope) {
		scope.withHierarchy( HibernateOrmSpecificAttributesMappingTest.MyEntity.class, (entityDescriptor) -> {
			Generator generator = entityDescriptor.getIdentifierProperty().createGenerator( null );
			assertThat( generator )
					.isInstanceOf( UuidGenerator.class );

			Property name = entityDescriptor.getProperty( "name" );
			assertThat( name.getValue() )
					.isInstanceOf( BasicValue.class );
			assertThat( ( (BasicValue) name.getValue() ).getExplicitJdbcTypeCode() )
					.isEqualTo( SqlTypes.CLOB );

			Property tags = entityDescriptor.getProperty( "tags" );
			assertThat( tags.getValue() )
					.isInstanceOf( BasicValue.class );
			assertThat( ( (BasicValue) tags.getValue() ).getResolution() )
					.isInstanceOf( UserTypeResolution.class );
		} );

		scope.withHierarchy( HibernateOrmSpecificAttributesMappingTest.MyEntityWithTenantId.class, (entityDescriptor) -> {
			Property tenantId = entityDescriptor.getProperty( "tenantId" );
			GeneratorCreator valueGeneratorCreator = tenantId.getValueGeneratorCreator();
			assertThat( valueGeneratorCreator ).isNotNull();
			Generator generator = valueGeneratorCreator.createGenerator( new CustomTenantGeneratorContext( tenantId ) );

			assertThat( generator )
					.isInstanceOf( TenantIdGeneration.class );

		} );
	}

	public static class MyEntity {
		private UUID id;

		private String name;

		private Set<String> tags;

		public UUID getId() {
			return id;
		}

		public void setId(UUID id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<String> getTags() {
			return tags;
		}

		public void setTags(Set<String> tags) {
			this.tags = tags;
		}
	}

	public static class MyEntityWithTenantId {
		private Long id;

		private String tenantId;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTenantId() {
			return tenantId;
		}

		public void setTenantId(String tenantId) {
			this.tenantId = tenantId;
		}
	}

	public static class DelimitedStringsJavaType extends UserTypeSupport<Set> {
		public DelimitedStringsJavaType() {
			super( Set.class, Types.VARCHAR );
		}
	}

	private static class CustomTenantGeneratorContext implements GeneratorCreationContext {
		private final Property tenantId;

		public CustomTenantGeneratorContext(Property tenantId) {
			this.tenantId = tenantId;
		}

		@Override
		public Database getDatabase() {
			return null;
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return null;
		}

		@Override
		public String getDefaultCatalog() {
			return null;
		}

		@Override
		public String getDefaultSchema() {
			return null;
		}

		@Override
		public PersistentClass getPersistentClass() {
			return tenantId.getPersistentClass();
		}

		@Override
		public Property getProperty() {
			return tenantId;
		}
	}
}
