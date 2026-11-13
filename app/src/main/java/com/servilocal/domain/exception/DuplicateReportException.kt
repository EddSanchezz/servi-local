package com.servilocal.domain.exception

class DuplicateReportException : Exception(
    "Este usuario ya ha reportado esta publicación. No se permiten reportes duplicados."
)
