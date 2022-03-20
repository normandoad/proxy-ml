CREATE TABLE cliente (
  id VARCHAR(255) NOT NULL,
  ip VARCHAR(255) NOT NULL,
  idproxy VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE consulta (
  id VARCHAR(255) NOT NULL,
  idcliente VARCHAR(255) NOT NULL,
  pathconsulta VARCHAR(255) NOT NULL,
  fechainicio TIMESTAMP NOT NULL,
  fechafin TIMESTAMP DEFAULT NULL,
  fechainicioconsultameli TIMESTAMP NOT NULL,
  fechafinconsultameli TIMESTAMP NOT NULL,
  excepcion VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE proxy (
  id VARCHAR(255) NOT NULL,
  puerto INTEGER,
  fechaencendido TIMESTAMP NOT NULL,
  fechaapagado TIMESTAMP DEFAULT NULL,
  excepcion VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY(id)
);
 
 ALTER TABLE consulta
    ADD FOREIGN KEY (idcliente) REFERENCES cliente(id);
 ALTER TABLE cliente  
    ADD FOREIGN KEY (idproxy) REFERENCES proxy(id);

   