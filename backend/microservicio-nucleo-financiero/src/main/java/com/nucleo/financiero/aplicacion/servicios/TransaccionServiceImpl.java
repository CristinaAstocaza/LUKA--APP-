package com.nucleo.financiero.aplicacion.servicios;

import com.nucleo.financiero.aplicacion.dtos.solicitudes.SolicitudTransaccion;
import com.nucleo.financiero.aplicacion.dtos.respuestas.ResumenFinancieroDTO;
import com.nucleo.financiero.aplicacion.dtos.respuestas.RespuestaTransaccion;
import com.nucleo.financiero.aplicacion.puertos.ITransaccionService;
import com.nucleo.financiero.aplicacion.mappers.TransaccionMapper;
import com.nucleo.financiero.dominio.entidades.Categoria;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import com.nucleo.financiero.dominio.entidades.Transaccion;
import com.nucleo.financiero.dominio.repositorios.CategoriaRepository;
import com.nucleo.financiero.dominio.repositorios.TransaccionRepository;
import com.nucleo.financiero.infraestructura.mensajeria.PublicadorAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import com.libreria.comun.excepciones.ExcepcionRecursoNoEncontrado;
import java.util.List;
import java.util.UUID;

/**
 * ImplementaciÃ³n de {@link ITransaccionService} para la gestiÃ³n de movimientos financieros.
 * <p>
 * Aplica lÃ³gica de negocio para la validaciÃ³n, persistencia y auditorÃ­a de transacciones,
 * integrando repositorios de dominio y publicadores de eventos.
 * </p>
 *
 * @version 1.3.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransaccionServiceImpl implements ITransaccionService {

    private final TransaccionRepository transaccionRepository;
    private final CategoriaRepository categoriaRepository;
    private final PublicadorAuditoria publicadorAuditoria;
    private final TransaccionMapper transaccionMapper;

    @Override
    @Transactional
    public RespuestaTransaccion registrar(SolicitudTransaccion request, String ipCliente) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de transacciÃ³n no puede ser nula.");
        }
        Transaccion guardada = transaccionRepository.save(construirEntidad(request));
        log.info("TransacciÃ³n registrada: {} â€” {} {} ({})",
                guardada.getId(), guardada.getTipo(), guardada.getMonto(), guardada.getNombreCliente());

        publicadorAuditoria.publicarRegistro(
                guardada.getUsuarioId(),
                guardada.getId(),
                guardada.getMonto().toString(),
                ipCliente
        );
        return transaccionMapper.aDto(guardada);
    }

    @Override
    @Transactional
    public List<RespuestaTransaccion> registrarLote(List<SolicitudTransaccion> solicitudes, String ipCliente) {
        if (solicitudes == null || solicitudes.isEmpty()) {
            throw new IllegalArgumentException("La lista de transacciones no puede estar vacÃ­a.");
        }
        if (solicitudes.size() > 500) {
            throw new IllegalArgumentException(
                    "El lote no puede superar 500 transacciones. Recibidas: " + solicitudes.size());
        }
        log.info("Iniciando registro en lote: {} transacciones", solicitudes.size());
        List<Transaccion> entidades = solicitudes.stream()
                .map(this::construirEntidad)
                .toList();
        List<Transaccion> guardadas = transaccionRepository.saveAll(entidades);
        log.info("Lote completado: {} transacciones guardadas", guardadas.size());
        
        publicadorAuditoria.publicarAcceso(
                guardadas.get(0).getUsuarioId(),
                "REGISTRO_LOTE_TRANSACCIONES",
                "Se registraron " + guardadas.size() + " transacciones exitosamente.",
                ipCliente
        );
        return guardadas.stream().map(transaccionMapper::aDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RespuestaTransaccion> listarHistorial(
            UUID usuarioId, TipoMovimiento tipo, UUID categoriaId,
            LocalDateTime desde, LocalDateTime hasta, Pageable paginacion,
            String ipCliente) {

        if (usuarioId == null) {
            throw new IllegalArgumentException("El ID de usuario no puede ser nulo.");
        }
        if (paginacion == null) {
            throw new IllegalArgumentException("La informaciÃ³n de paginaciÃ³n no puede ser nula.");
        }

        if (desde == null) {
            desde = LocalDateTime.now().minusDays(30);
        }
        if (hasta == null) {
            hasta = LocalDateTime.now();
        }

        publicadorAuditoria.publicarAcceso(usuarioId, "CONSULTA_HISTORIAL",
                "Rango: " + desde + " a " + hasta, ipCliente);

        org.springframework.data.jpa.domain.Specification<Transaccion> specs = org.springframework.data.jpa.domain.Specification
                .where(com.nucleo.financiero.dominio.especificaciones.TransaccionSpecs.porUsuario(usuarioId))
                .and(com.nucleo.financiero.dominio.especificaciones.TransaccionSpecs.porTipo(tipo))
                .and(com.nucleo.financiero.dominio.especificaciones.TransaccionSpecs.porCategoria(categoriaId))
                .and(com.nucleo.financiero.dominio.especificaciones.TransaccionSpecs.entreFechas(desde, hasta));

        return transaccionRepository.findAll(specs, paginacion).map(transaccionMapper::aDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenFinancieroDTO obtenerResumen(UUID usuarioId, Integer mes, Integer anio, String ipCliente) {
        LocalDateTime[] rango = resolverRangoFechas(mes, anio);
        LocalDateTime desde = rango[0];
        LocalDateTime hasta = rango[1];

        BigDecimal totalIngresos = transaccionRepository.sumarIngresosPorPeriodo(usuarioId, desde, hasta);
        BigDecimal totalGastos = transaccionRepository.sumarGastosPorPeriodo(usuarioId, desde, hasta);
        long cantidadIngresos = transaccionRepository.contarPorTipoYPeriodo(usuarioId, TipoMovimiento.INGRESO, desde, hasta);
        long cantidadGastos = transaccionRepository.contarPorTipoYPeriodo(usuarioId, TipoMovimiento.GASTO, desde, hasta);

        publicadorAuditoria.publicarAcceso(
                usuarioId,
                "OBTENER_RESUMEN",
                "Se generÃ³ el resumen financiero del periodo solicitado.",
                ipCliente
        );
        return ResumenFinancieroDTO.calcular(desde, hasta, totalIngresos, totalGastos, cantidadIngresos, cantidadGastos);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaTransaccion obtenerPorId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de transacciÃ³n no puede ser nulo.");
        }
        return transaccionRepository.findById(id)
                .map(transaccionMapper::aDto)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Transaccion", id));
    }

    /**
     * Construye una entidad de dominio {@link Transaccion} a partir de una solicitud DTO.
     * Realiza validaciones de integridad entre categorÃ­a y tipo de movimiento.
     * 
     * @param request Datos de la solicitud.
     * @return Entidad de dominio construida.
     * @throws NoSuchElementException Si la categorÃ­a no existe.
     * @throws IllegalStateException Si hay inconsistencia entre categorÃ­a y tipo.
     */
    private Transaccion construirEntidad(SolicitudTransaccion request) {
        Categoria categoria = obtenerCategoriaValidada(request.categoriaId());
        validarConsistenciaTransaccion(request, categoria);
        
        return Transaccion.builder()
                .usuarioId(request.usuarioId())
                .nombreCliente(request.nombreCliente())
                .monto(request.monto())
                .tipo(request.tipo())
                .categoria(categoria)
                .fechaTransaccion(request.fechaTransaccion() != null ? request.fechaTransaccion() : LocalDateTime.now())
                .metodoPago(request.metodoPago())
                .etiquetas(request.etiquetas())
                .descripcion(request.descripcion())
                .build();
    }

    /**
     * Obtiene y valida la categorÃ­a asociada a la transacciÃ³n.
     */
    private Categoria obtenerCategoriaValidada(UUID categoriaId) {
        if (categoriaId == null) {
            throw new IllegalArgumentException("El ID de la categorÃ­a es nulo en la peticiÃ³n.");
        }
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Categoria", categoriaId));
    }

    /**
     * Valida la consistencia de tipos entre la transacciÃ³n y la categorÃ­a seleccionada.
     */
    private void validarConsistenciaTransaccion(SolicitudTransaccion request, Categoria categoria) {
        if (request.tipo() == null) {
            throw new IllegalArgumentException("El tipo de movimiento es obligatorio.");
        }
        if (categoria.getTipo() != request.tipo()) {
            throw new IllegalStateException(String.format(
                    "Inconsistencia: La categorÃ­a es de tipo %s pero la transacciÃ³n es %s.",
                    categoria.getTipo(), request.tipo()));
        }
    }

    /**
     * Resuelve el rango de fechas para un mes y aÃ±o especÃ­ficos.
     * 
     * @param mes Mes (1-12).
     * @param anio AÃ±o (ej: 2026).
     * @return Array con fecha inicio [0] y fecha fin [1].
     */
    private LocalDateTime[] resolverRangoFechas(Integer mes, Integer anio) {
        int anioResuelto = (anio != null) ? anio : LocalDateTime.now().getYear();
        int mesResuelto = (mes != null) ? mes : LocalDateTime.now().getMonthValue();
        YearMonth periodo = YearMonth.of(anioResuelto, mesResuelto);

        return new LocalDateTime[]{
            periodo.atDay(1).atStartOfDay(),
            LocalDateTime.now()
        };
    }
}
