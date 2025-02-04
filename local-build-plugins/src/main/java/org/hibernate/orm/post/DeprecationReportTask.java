/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Comparator;
import java.util.TreeSet;

import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;

import org.jboss.jandex.DotName;

import static org.hibernate.orm.post.ReportGenerationPlugin.TASK_GROUP_NAME;

/**
 * @author Steve Ebersole
 */
public class DeprecationReportTask extends AbstractJandexAwareTask {
	public static final String REMOVE_ANN_NAME = "org.hibernate.Remove";
	public static final String DEPRECATED_ANN_NAME = Deprecated.class.getName();

	private final Property<RegularFile> reportFile;

	public DeprecationReportTask() {
		setDescription( "Generates a report for things considered deprecated" );
		reportFile = getProject().getObjects().fileProperty();
		reportFile.convention( getProject().getLayout().getBuildDirectory().file( "orm/reports/deprecating.txt" ) );
	}

	@Override
	protected Provider<RegularFile> getTaskReportFileReference() {
		return reportFile;
	}

	@TaskAction
	public void generateIncubationReport() {
		final TreeSet<Inclusion> deprecations = new TreeSet<>( Comparator.comparing( Inclusion::getPath ) );

		processAnnotations(
				deprecations::add,
				DotName.createSimple( REMOVE_ANN_NAME ),
				DotName.createSimple( DEPRECATED_ANN_NAME )
		);

		// NOTE : at this point, `deprecations` contains a set of deprecation
		// names ordered alphabetically..

		writeReport( deprecations );
	}

	@Override
	protected void writeReportHeader(OutputStreamWriter fileWriter) {
		super.writeReportHeader( fileWriter );

		try {
			fileWriter.write( "# All API elements considered deprecated - union of @Deprecated and @Remove" );
			fileWriter.write( '\n' );
			fileWriter.write( '\n' );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}
}
