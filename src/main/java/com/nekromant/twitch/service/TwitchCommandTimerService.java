package com.nekromant.twitch.service;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.helix.domain.Stream;
import com.nekromant.twitch.model.TwitchCommand;
import com.nekromant.twitch.repository.TwitchCommandRepository;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Slf4j
@Setter
@Component
public class TwitchCommandTimerService {
    @Autowired
    private TwitchCommandRepository twitchCommandRepository;
    @Autowired
    private TwitchAuthService twitchAuthService;
    @Value("${twitch.channelName}")
    private String channelName;
    private Long NOT_PERIOD_EXECUTION = 0L;

    public TwitchCommandTimerService() {
    }

    public void executedCommandsByTime(TwitchClient twitchClient) {
        if (isLiveStream(twitchClient)) {

            List<TwitchCommand> twitchCommands = twitchCommandRepository
                    .findAllByPeriodNotAndEnabledIsTrue(NOT_PERIOD_EXECUTION);

            for (TwitchCommand twitchCommand : twitchCommands) {
                if (checkPeriod(twitchCommand)) {
                    twitchClient.getChat().sendMessage(channelName, twitchCommand.getResponse());

                    twitchCommand.setLastCompletionTime(Instant.now());
                    twitchCommandRepository.save(twitchCommand);
                    log.info("Running task for command: !" + twitchCommand.getName());
                }
            }
        }
    }

    private boolean checkPeriod(TwitchCommand twitchCommand) {
        if (twitchCommand.getLastCompletionTime() == null) {
            return true;
        }
        long time = Duration.between(twitchCommand.getLastCompletionTime(), Instant.now()).toMinutes();
        Long timeCommand = twitchCommand.getPeriod();
        System.out.println("Разница между первым запуском " + twitchCommand.getName() + " для команды: " + time + " Период команды: " + timeCommand);

        return time
                >= timeCommand;
    }

    private boolean isLiveStream(TwitchClient twitchClient) {
        List<Stream> streams = twitchClient.getHelix().getStreams(twitchAuthService.getAuthToken()
                                .getAccessToken(), null, null, 1, null, null, null,
                        Collections.singletonList(channelName))
                .execute().getStreams();
        return !streams.isEmpty();
    }
}
