package order_service.service;
import order_service.dto.ProductSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
@Component
public class ProductClient {
    private final RestClient client;
    public ProductClient(RestClient.Builder builder,@Value("${app.product-service.base-url}") String baseUrl){
        client=builder.baseUrl(baseUrl).build();
    }
    public ProductSnapshot getProduct(Long productId){
        try{
            ProductSnapshot p=client.get().uri("/api/products/{id}",productId).retrieve().body(ProductSnapshot.class);
            if(p==null||p.id()==null||p.price()==null||p.name()==null)
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Product service returned invalid data");
            return p;
        }catch(RestClientResponseException ex){
            if(ex.getStatusCode().value()==404) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Product does not exist or is inactive");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Product service request failed");
        }catch(RestClientException ex){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Product service is unavailable");
        }
    }
}
