/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.instrument.runtime;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.BytecodeProvider;
import org.hibernate.bytecode.InstrumentedClassLoader;
import org.hibernate.bytecode.util.BasicClassFilter;
import org.hibernate.bytecode.util.FieldFilter;
import org.hibernate.testing.junit4.BaseClassLoaderIsolatedTestCase;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTransformingClassLoaderInstrumentTestCase extends BaseClassLoaderIsolatedTestCase {

	protected ClassLoader buildIsolatedClassLoader(ClassLoader parent) {
		BytecodeProvider provider = buildBytecodeProvider();
		return new InstrumentedClassLoader(
				parent,
				provider.getTransformer(
						new BasicClassFilter( new String[] { "org.hibernate.test.instrument" }, null ),
						new FieldFilter() {
							public boolean shouldInstrumentField(String className, String fieldName) {
								return className.startsWith( "org.hibernate.test.instrument.domain" );
							}
							public boolean shouldTransformFieldAccess(String transformingClassName, String fieldOwnerClassName, String fieldName) {
								return fieldOwnerClassName.startsWith( "org.hibernate.test.instrument.domain" )
										&& transformingClassName.equals( fieldOwnerClassName );
							}
						}
				)
		);

	}

	protected void releaseIsolatedClassLoader(ClassLoader isolatedLoader) {
	}

	protected abstract BytecodeProvider buildBytecodeProvider();


	// the tests ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Test
	public void testSetFieldInterceptor() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestInjectFieldInterceptorExecutable" );
	}

	@Test
	public void testDirtyCheck() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestDirtyCheckExecutable" );
	}

	@Test
	public void testFetchAll() throws Exception {
		executeExecutable( "org.hibernate.test.instrument.cases.TestFetchAllExecutable" );
	}

	@Test
	public void testLazy() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestLazyExecutable" );
	}

	@Test
	public void testLazyManyToOne() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestLazyManyToOneExecutable" );
	}

	@Test
	public void testPropertyInitialized() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestIsPropertyInitializedExecutable" );
	}

	@Test
	public void testManyToOneProxy() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestManyToOneProxyExecutable" );
	}

	@Test
	public void testLazyPropertyCustomType() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestLazyPropertyCustomTypeExecutable" );
	}

	@Test
	public void testSharedPKOneToOne() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestSharedPKOneToOneExecutable" );
	}

	@Test
	public void testCustomColumnReadAndWrite() {
		executeExecutable( "org.hibernate.test.instrument.cases.TestCustomColumnReadAndWrite" );
	}	

	// reflection code to ensure isolation into the created classloader ~~~~~~~

	private static final Class[] SIG = new Class[] {};
	private static final Object[] ARGS = new Object[] {};

	public void executeExecutable(String name) {
		Class execClass = null;
		Object executable = null;
		try {
			execClass = Thread.currentThread().getContextClassLoader().loadClass( name );
			executable = execClass.newInstance();
		}
		catch( Throwable t ) {
			throw new HibernateException( "could not load executable", t );
		}
		try {
			execClass.getMethod( "prepare", SIG ).invoke( executable, ARGS );
			execClass.getMethod( "execute", SIG ).invoke( executable, ARGS );
		}
		catch ( NoSuchMethodException e ) {
			throw new HibernateException( "could not exeucte executable", e );
		}
		catch ( IllegalAccessException e ) {
			throw new HibernateException( "could not exeucte executable", e );
		}
		catch ( InvocationTargetException e ) {
			throw new HibernateException( "could not exeucte executable", e.getTargetException() );
		}
		finally {
			try {
				execClass.getMethod( "complete", SIG ).invoke( executable, ARGS );
			}
			catch ( Throwable ignore ) {
			}
		}
	}
}