template.project = 'consent'

template.elastic_ip = case environment
		      when 'ci'
			        '54.225.223.26'
		      when 'production'
			        ''
		      end

template.conjurtype = "consent"
