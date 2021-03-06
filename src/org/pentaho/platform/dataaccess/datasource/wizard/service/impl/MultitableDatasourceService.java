/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2011 Pentaho Corporation..  All rights reserved.
 * 
 * @author Ezequiel Cuellar
 */
package org.pentaho.platform.dataaccess.datasource.wizard.service.impl;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.agilebi.modeler.ModelerException;
import org.pentaho.agilebi.modeler.gwt.BogoPojo;
import org.pentaho.agilebi.modeler.util.MultiTableModelerSource;
import org.pentaho.database.model.IDatabaseConnection;
import org.pentaho.database.util.DatabaseUtil;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.metadata.model.Domain;
import org.pentaho.platform.dataaccess.datasource.IConnection;
import org.pentaho.platform.dataaccess.datasource.wizard.IDatasourceSummary;
import org.pentaho.platform.dataaccess.datasource.wizard.service.ConnectionServiceException;
import org.pentaho.platform.dataaccess.datasource.wizard.service.DatasourceServiceException;
import org.pentaho.platform.dataaccess.datasource.wizard.service.gwt.IGwtJoinSelectionService;
import org.pentaho.platform.dataaccess.datasource.wizard.service.impl.utils.ConnectionServiceHelper;
import org.pentaho.platform.dataaccess.datasource.wizard.sources.query.QueryDatasourceSummary;
import org.pentaho.platform.engine.core.system.PentahoBase;

import com.thoughtworks.xstream.XStream;

public class MultitableDatasourceService extends PentahoBase implements IGwtJoinSelectionService {
	
	private DatabaseMeta databaseMeta;
	private ConnectionServiceImpl connectionServiceImpl;
  private Log logger = LogFactory.getLog(MultitableDatasourceService.class);
	
	public MultitableDatasourceService() {
		this.connectionServiceImpl = new ConnectionServiceImpl();
	}

	public MultitableDatasourceService(DatabaseMeta databaseMeta) {
		this.databaseMeta = databaseMeta;
	}
	
	private DatabaseMeta getDatabaseMeta(IConnection connection) throws ConnectionServiceException {
		if(this.connectionServiceImpl == null) {
			return this.databaseMeta;
		}
		
		IDatabaseConnection iDatabaseConnection = this.connectionServiceImpl.convertFromConnection(connection);
		iDatabaseConnection.setPassword(ConnectionServiceHelper.getConnectionPassword(connection.getName(), connection.getPassword()));
		return DatabaseUtil.convertToDatabaseMeta(iDatabaseConnection);
  }

	public List<String> getDatabaseTables(IConnection connection) throws DatasourceServiceException {
    try{
      DatabaseMeta databaseMeta = this.getDatabaseMeta(connection);
      Database database = new Database(null, databaseMeta);
      database.connect();

      String[] tableNames = database.getTablenames();
      List<String> tables = Arrays.asList(tableNames);
      database.disconnect();
      return tables;
    } catch (KettleDatabaseException e) {
      logger.error("Error creating database object", e);
      throw new DatasourceServiceException(e);
    } catch (ConnectionServiceException e) {
      logger.error("Error getting database meta", e);
      throw new DatasourceServiceException(e);
    }
  }

	public IDatasourceSummary serializeJoins(MultiTableDatasourceDTO dto, IConnection connection) throws DatasourceServiceException {
    try{
      ModelerService modelerService = new ModelerService();
      modelerService.initKettle();

      DatabaseMeta databaseMeta = this.getDatabaseMeta(connection);
      MultiTableModelerSource multiTable = new MultiTableModelerSource(databaseMeta, dto.getSchemaModel(), dto.getDatasourceName(), dto.getSelectedTables());
      Domain domain = multiTable.generateDomain(dto.isDoOlap());
      domain.getLogicalModels().get(0).setProperty("datasourceModel", serializeModelState(dto));
      domain.getLogicalModels().get(0).setProperty("DatasourceType", "MULTI-TABLE-DS");
      modelerService.serializeModels(domain, dto.getDatasourceName(), dto.isDoOlap());

      QueryDatasourceSummary summary = new QueryDatasourceSummary();
      summary.setDomain(domain);
      return summary;
    } catch (Exception e) {
      logger.error("Error serializing joins", e);
      throw new DatasourceServiceException(e);
    }
  }

	private String serializeModelState(MultiTableDatasourceDTO dto) throws DatasourceServiceException {
		XStream xs = new XStream();
		return xs.toXML(dto);
	}

	public MultiTableDatasourceDTO deSerializeModelState(String dtoStr) throws DatasourceServiceException {
		try {
			XStream xs = new XStream();
			return (MultiTableDatasourceDTO) xs.fromXML(dtoStr);
		} catch (Exception e) {
      logger.error(e);
			throw new DatasourceServiceException(e);
		}
	}

	public List<String> getTableFields(String table, IConnection connection) throws DatasourceServiceException {
    try{
      DatabaseMeta databaseMeta = this.getDatabaseMeta(connection);
      Database database = new Database(null, databaseMeta);
      database.connect();

      String query = databaseMeta.getSQLQueryFields(table);
      database.getRows(query, 1);
      String[] tableFields = database.getReturnRowMeta().getFieldNames();

      List<String> fields = Arrays.asList(tableFields);
      database.disconnect();
      return fields;
    } catch (KettleDatabaseException e) {
      logger.error(e);
      throw new DatasourceServiceException(e);
    } catch (ConnectionServiceException e) {
      logger.error(e);
      throw new DatasourceServiceException(e);
    }
  }

	public BogoPojo gwtWorkaround(BogoPojo pojo) {
		return pojo;
	}

	@Override
	public Log getLogger() {
		// TODO Auto-generated method stub
		return null;
	}
}
