package com.steve.db.data.manager;

public class DAOFactory {
  public static DAO createDAO(DAO clazz) /*throws CsistRuntimeException*/
  {
      DAO dao = (DAO) TransactionProxy.newInstance(clazz);
      return dao;
  }

}
