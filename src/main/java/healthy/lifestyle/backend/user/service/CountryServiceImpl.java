package healthy.lifestyle.backend.user.service;

import healthy.lifestyle.backend.shared.exception.ApiException;
import healthy.lifestyle.backend.shared.exception.ErrorMessage;
import healthy.lifestyle.backend.user.dto.CountryResponseDto;
import healthy.lifestyle.backend.user.model.Country;
import healthy.lifestyle.backend.user.repository.CountryRepository;
import java.util.Comparator;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CountryServiceImpl implements CountryService {
    @Autowired
    CountryRepository countryRepository;

    @Autowired
    ModelMapper modelMapper;

    @Override
    public List<CountryResponseDto> getCountries() {
        List<Country> countries = countryRepository.findAll();
        if (countries.isEmpty()) throw new ApiException(ErrorMessage.NOT_FOUND, null, HttpStatus.NOT_FOUND);

        return countries.stream()
                .map(country -> modelMapper.map(country, CountryResponseDto.class))
                .sorted(Comparator.comparing(CountryResponseDto::getName))
                .toList();
    }
}
