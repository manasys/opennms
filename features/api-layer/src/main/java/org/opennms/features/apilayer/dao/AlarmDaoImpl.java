/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.apilayer.dao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.opennms.core.criteria.Criteria;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.core.criteria.restrictions.Restrictions;
import org.opennms.features.apilayer.utils.ModelMappers;
import org.opennms.integration.api.v1.dao.AlarmDao;
import org.opennms.integration.api.v1.graph.NodeRef;
import org.opennms.integration.api.v1.model.Alarm;
import org.opennms.netmgt.dao.api.SessionUtils;
import org.opennms.netmgt.model.OnmsAlarm;

public class AlarmDaoImpl implements AlarmDao {

    private final org.opennms.netmgt.dao.api.AlarmDao alarmDao;
    private final SessionUtils sessionUtils;

    public AlarmDaoImpl(org.opennms.netmgt.dao.api.AlarmDao alarmDao, SessionUtils sessionUtils) {
        this.alarmDao = Objects.requireNonNull(alarmDao);
        this.sessionUtils = Objects.requireNonNull(sessionUtils);
    }

    @Override
    public Long getAlarmCount() {
        final CriteriaBuilder criteriaBuilder = new CriteriaBuilder(OnmsAlarm.class);
        return sessionUtils.withReadOnlyTransaction(() -> (long)alarmDao.countMatching(criteriaBuilder.toCriteria()));
    }

    @Override
    public List<Alarm> getAlarms() {
        return sessionUtils.withReadOnlyTransaction(() ->
                alarmDao.findAll().stream().map(ModelMappers::toAlarm).collect(Collectors.toList()));
    }

    @Override
    public Optional<Alarm> getAlarmWithHighestSeverity(NodeRef nodeRef) {
        final Criteria criteria = new CriteriaBuilder(OnmsAlarm.class)
                .alias("node", "node")
                .orderBy("severity", false)
                .and(Restrictions.eq("node.foreignSource", nodeRef.getForeignSource()),
                     Restrictions.eq("node.foreignId", nodeRef.getForeignId()))
                .limit(1)
                .toCriteria();
        return sessionUtils.withReadOnlyTransaction(() -> {
            final List<OnmsAlarm> matching = alarmDao.findMatching(criteria);
            if (matching.isEmpty()) {
                return Optional.empty();
            }
            final Alarm alarm = ModelMappers.toAlarm(matching.get(0));
            return Optional.of(alarm);
        });
    }
}
