/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.joinedsubclassbatch;

import java.io.Serializable;
import java.math.BigDecimal;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;

/**
 * Test batching of insert,update,delete on joined subclasses
 *
 * @author dcebotarenco
 */
@TestForIssue(jiraKey = "HHH-2558")
@DomainModel(
		annotatedClasses = {
				JoinedSubclassBatchingTest.Person.class,
				JoinedSubclassBatchingTest.Employee.class,
				JoinedSubclassBatchingTest.Customer.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = Environment.STATEMENT_BATCH_SIZE, value = "20")
)
public class JoinedSubclassBatchingTest {

	@Test
	public void doBatchInsertUpdateJoinedSubclassNrEqualWithBatch(SessionFactoryScope scope) {
		doBatchInsertUpdateJoined( 20, 20, scope );
	}

	@Test
	public void doBatchInsertUpdateJoinedSubclassNrLessThenBatch(SessionFactoryScope scope) {
		doBatchInsertUpdateJoined( 19, 20, scope );
	}

	@Test
	public void doBatchInsertUpdateJoinedSubclassNrBiggerThenBatch(SessionFactoryScope scope) {
		doBatchInsertUpdateJoined( 21, 20, scope );
	}

	@Test
	public void testBatchInsertUpdateSizeEqJdbcBatchSize(SessionFactoryScope scope) {
		int batchSize = scope.getSessionFactory().getSessionFactoryOptions().getJdbcBatchSize();
		doBatchInsertUpdateJoined( 50, batchSize, scope );
	}

	@Test
	public void testBatchInsertUpdateSizeLtJdbcBatchSize(SessionFactoryScope scope) {
		int batchSize = scope.getSessionFactory().getSessionFactoryOptions().getJdbcBatchSize();
		doBatchInsertUpdateJoined( 50, batchSize - 1, scope );
	}

	@Test
	public void testBatchInsertUpdateSizeGtJdbcBatchSize(SessionFactoryScope scope) {
		int batchSize = scope.getSessionFactory().getSessionFactoryOptions().getJdbcBatchSize();
		doBatchInsertUpdateJoined( 50, batchSize + 1, scope );
	}

	public void doBatchInsertUpdateJoined(int nEntities, int nBeforeFlush, SessionFactoryScope scope) {

		scope.inTransaction( s -> {
			for ( int i = 0; i < nEntities; i++ ) {
				Employee e = new Employee();
				e.getId();
				e.setName( "Mark" );
				e.setTitle( "internal sales" );
				e.setSex( 'M' );
				e.setAddress( "buckhead" );
				e.setZip( "30305" );
				e.setCountry( "USA" );
				s.save( e );
				if ( i % nBeforeFlush == 0 && i > 0 ) {
					s.flush();
					s.clear();
				}
			}
		} );

		scope.inTransaction( s -> {
			try (ScrollableResults sr = s.createQuery(
							"select e from Employee e" )
					.scroll( ScrollMode.FORWARD_ONLY )) {

				while ( sr.next() ) {
					Employee e = (Employee) sr.get();
					e.setTitle( "Unknown" );
				}
			}
		} );

		scope.inTransaction( s -> {
			try (ScrollableResults sr = s.createQuery(
							"select e from Employee e" )
					.scroll( ScrollMode.FORWARD_ONLY )) {

				while ( sr.next() ) {
					Employee e = (Employee) sr.get();
					s.delete( e );
				}
			}
		} );
	}

	@Embeddable
	public static class Address implements Serializable {

		public String address;

		public String zip;

		public String country;

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public String getZip() {
			return zip;
		}

		public void setZip(String zip) {
			this.zip = zip;
		}

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}
	}

	@Entity(name = "Customer")
	public static class Customer extends Person {

		@ManyToOne(fetch = FetchType.LAZY)
		private Employee salesperson;

		private String comments;

		public Employee getSalesperson() {
			return salesperson;
		}

		public void setSalesperson(Employee salesperson) {
			this.salesperson = salesperson;
		}

		public String getComments() {
			return comments;
		}

		public void setComments(String comments) {
			this.comments = comments;
		}
	}

	@Entity(name = "Employee")
	public static class Employee extends Person {

		@Column(nullable = false, length = 20)
		private String title;

		private BigDecimal salary;

		private double passwordExpiryDays;

		@ManyToOne(fetch = FetchType.LAZY)
		private Employee manager;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public Employee getManager() {
			return manager;
		}

		public void setManager(Employee manager) {
			this.manager = manager;
		}

		public BigDecimal getSalary() {
			return salary;
		}

		public void setSalary(BigDecimal salary) {
			this.salary = salary;
		}

		public double getPasswordExpiryDays() {
			return passwordExpiryDays;
		}

		public void setPasswordExpiryDays(double passwordExpiryDays) {
			this.passwordExpiryDays = passwordExpiryDays;
		}
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Person {

		@Id
		@GeneratedValue(generator = "system-uuid")
		@GenericGenerator(name = "system-uuid", strategy = "uuid2")
		private String id;

		@Column(nullable = false, length = 80)
		private String name;

		@Column(nullable = false, updatable = false)
		private char sex;

		@jakarta.persistence.Version
		private int version;

		private double heightInches;

		@Embedded
		private Address address = new Address();

		public Address getAddress() {
			return address;
		}

		public void setAddress(String string) {
			this.address.address = string;
		}

		public void setZip(String string) {
			this.address.zip = string;
		}

		public void setCountry(String string) {
			this.address.country = string;
		}

		public char getSex() {
			return sex;
		}

		public void setSex(char sex) {
			this.sex = sex;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String identity) {
			this.name = identity;
		}

		public double getHeightInches() {
			return heightInches;
		}

		public void setHeightInches(double heightInches) {
			this.heightInches = heightInches;
		}

		public int getVersion() {
			return version;
		}

		public void setVersion(int version) {
			this.version = version;
		}
	}
}

