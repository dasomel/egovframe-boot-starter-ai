package org.egovframe.boot.ai.prompt;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 외부 파일에서 프롬프트 템플릿을 로드하고 변수를 치환하여 문자열을 반환한다.
 * 로드된 템플릿은 ConcurrentHashMap에 캐시하여 재사용한다.
 */
public class EgovPromptTemplateManager {

    private final ResourceLoader resourceLoader;
    private final EgovPromptTemplateProperties properties;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public EgovPromptTemplateManager(ResourceLoader resourceLoader,
                                     EgovPromptTemplateProperties properties) {
        this.resourceLoader = resourceLoader;
        this.properties = properties;
    }

    /**
     * 템플릿 이름으로 리소스를 로드하여 variables로 플레이스홀더를 치환한 문자열을 반환한다.
     *
     * @param name      템플릿 이름(파일명에서 suffix 제외)
     * @param variables 치환할 변수 맵
     * @return 치환 완료된 문자열
     * @throws IllegalArgumentException 템플릿 리소스를 찾을 수 없거나 읽을 수 없을 때
     */
    public String render(String name, Map<String, Object> variables) {
        String templateText = cache.computeIfAbsent(name, this::loadTemplate);
        return new PromptTemplate(templateText).render(variables);
    }

    /**
     * 해당 이름의 템플릿이 존재하는지 확인한다.
     */
    public boolean contains(String name) {
        if (cache.containsKey(name)) {
            return true;
        }
        String location = resolveLocation(name);
        Resource resource = resourceLoader.getResource(location);
        return resource.exists();
    }

    private String loadTemplate(String name) {
        String location = resolveLocation(name);
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalArgumentException(
                    "프롬프트 템플릿을 찾을 수 없습니다: name='" + name + "', location='" + location + "'");
        }
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "프롬프트 템플릿을 읽을 수 없습니다: name='" + name + "', location='" + location + "'", e);
        }
    }

    private String resolveLocation(String name) {
        String base = properties.getLocation();
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        return base + name + properties.getSuffix();
    }
}
