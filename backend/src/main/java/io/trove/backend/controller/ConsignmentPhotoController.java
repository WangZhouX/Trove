package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.trove.backend.api.ConsignmentPhotoApi;
import io.trove.backend.api.ConsignmentPhotoException;
import io.trove.backend.service.ConsignmentPhotoService;
import io.trove.backend.service.WalletSessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/** 寄存人和库管员的私有照片 HTTP 入口。 / HTTP routes for private photos belonging to depositors and warehouse operators. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "寄存私有照片")
public class ConsignmentPhotoController {

    /** 上传、索引和鉴权读取服务。 / Service for uploads, listing and authorized reads. */
    private final ConsignmentPhotoService photos;

    /**
     * 注入私有照片服务。 / Injects the private photo service.
     *
     * @param photos 寄存人及库管员的照片服务 / photo service for depositors and warehouse operators
     */
    public ConsignmentPhotoController(ConsignmentPhotoService photos) {
        this.photos = photos;
    }

    /**
     * 「我的寄存」待寄入详情发起补传时使用。每次成功追加一张照片，待前端联调。
     * My Consignments calls this route to append one photo to an awaiting-arrival application. UI integration is pending.
     *
     * @param identity 当前寄存人的认证会话 / authenticated depositor session
     * @param consignmentId 该钱包拥有的寄存单编号 / consignment ID owned by the wallet
     * @param request 请求体为原始 JPEG 或 PNG，不使用 multipart / raw JPEG or PNG request body without multipart
     * @return 201、私有照片读取路径和新照片条目 / 201 with the private photo URL and new entry
     */
    @PostMapping(value = "/consignments/{consignmentId}/photos", consumes = {
            MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE
    })
    @Operation(summary = "上传本人寄存照片", security = @SecurityRequirement(name = "walletBearer"),
            requestBody = @RequestBody(required = true, content = {
                    @Content(mediaType = MediaType.IMAGE_JPEG_VALUE, schema = @Schema(type = "string", format = "binary")),
                    @Content(mediaType = MediaType.IMAGE_PNG_VALUE, schema = @Schema(type = "string", format = "binary"))
            }))
    public ResponseEntity<ConsignmentPhotoApi.Photo> upload(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity,
            @PathVariable UUID consignmentId, @Parameter(hidden = true) HttpServletRequest request) {
        // 在受控服务中读取限量字节，避免请求绑定先把整个图像载入内存。
        // Read limited bytes inside the service instead of letting request binding buffer the entire image.
        try {
            var photo = photos.upload(identity, consignmentId, request.getInputStream(), request.getContentLengthLong());
            var location = URI.create("/api/v1/consignments/" + consignmentId + "/photos/" + photo.photoId());
            return ResponseEntity.created(location).cacheControl(CacheControl.noStore()).body(photo);
        } catch (IOException e) {
            throw new ConsignmentPhotoException(ConsignmentPhotoException.Error.INVALID_PHOTO_REQUEST);
        }
    }

    /**
     * 「我的寄存」详情展开时读取本人照片条目，待前端联调。
     * My Consignments loads the owner's photo entries when details open. UI integration is pending.
     *
     * @param identity 当前寄存人的认证会话 / authenticated depositor session
     * @param consignmentId 本人寄存单编号 / owned consignment ID
     * @return 用于展示和鉴权加载的照片编号、类型和顺序 / photo IDs, MIME and order for authorized viewing
     */
    @GetMapping("/consignments/{consignmentId}/photos")
    @Operation(summary = "查询本人寄存照片", security = @SecurityRequirement(name = "walletBearer"))
    public ResponseEntity<List<ConsignmentPhotoApi.Photo>> listMine(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity,
            @PathVariable UUID consignmentId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(photos.list(identity, consignmentId, false));
    }

    /**
     * 「我的寄存」详情需要显示某张图片时，携带钱包会话请求原图，待前端联调。
     * My Consignments uses the wallet session to load an individual original photo. UI integration is pending.
     *
     * @param identity 当前寄存人的认证会话 / authenticated depositor session
     * @param consignmentId 本人寄存单编号 / owned consignment ID
     * @param photoId 条目对应的照片编号 / ID from the photo entry
     * @return 已校验的私有图片字节 / verified private image bytes
     */
    @GetMapping("/consignments/{consignmentId}/photos/{photoId}")
    @Operation(summary = "查看本人寄存照片", security = @SecurityRequirement(name = "walletBearer"))
    public ResponseEntity<byte[]> getMine(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity,
            @PathVariable UUID consignmentId, @PathVariable UUID photoId) {
        return imageResponse(photos.read(identity, consignmentId, photoId, false));
    }

    /**
     * 「仓库工作台 › 入库与验真」打开寄存详情时加载照片条目，待前端联调。
     * Warehouse Intake and Inspection loads photo entries when opening a consignment. UI integration is pending.
     *
     * @param identity 已授权的库管员会话 / authorized warehouse operator session
     * @param consignmentId 当前验真的寄存单编号 / consignment ID selected for inspection
     * @return 显示图像所需的私有照片编号和顺序 / private photo IDs and display order
     */
    @GetMapping("/warehouse/consignments/{consignmentId}/photos")
    @Operation(summary = "查询仓库验真照片", security = @SecurityRequirement(name = "walletBearer"))
    public ResponseEntity<List<ConsignmentPhotoApi.Photo>> listForWarehouse(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity,
            @PathVariable UUID consignmentId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(photos.list(identity, consignmentId, true));
    }

    /**
     * 「仓库工作台 › 入库与验真」查看某张原图时发送仓库会话，待前端联调。
     * Warehouse Intake and Inspection sends its session when viewing an original photo. UI integration is pending.
     *
     * @param identity 已授权的库管员会话 / authorized warehouse operator session
     * @param consignmentId 当前验真的寄存单编号 / consignment ID selected for inspection
     * @param photoId 条目对应的照片编号 / ID from the photo entry
     * @return 已校验的私有图片字节 / verified private image bytes
     */
    @GetMapping("/warehouse/consignments/{consignmentId}/photos/{photoId}")
    @Operation(summary = "查看仓库验真照片", security = @SecurityRequirement(name = "walletBearer"))
    public ResponseEntity<byte[]> getForWarehouse(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity,
            @PathVariable UUID consignmentId, @PathVariable UUID photoId) {
        return imageResponse(photos.read(identity, consignmentId, photoId, true));
    }

    /**
     * 使用验证后保存的 MIME 输出原图，禁止缓存或类型嗅探。
     * Serves the original using its verified MIME type and disables caching and content sniffing.
     *
     * @param content 已鉴权的私有图像 / authorized private image
     * @return 带私有内容缓存控制的二进制应答 / binary response with private-content cache control
     */
    private static ResponseEntity<byte[]> imageResponse(ConsignmentPhotoApi.Content content) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(content.contentType()))
                .contentLength(content.bytes().length).body(content.bytes());
    }
}
