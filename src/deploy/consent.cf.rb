template.project = 'consent'

template.elastic_ip = case environment
		      when 'ci'
			        '54.225.223.26'
		      when 'production'
			        '54.163.249.21'
		      end

template.conjurtype = "consent"
